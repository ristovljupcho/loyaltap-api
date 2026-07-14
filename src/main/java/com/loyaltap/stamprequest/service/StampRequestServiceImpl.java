package com.loyaltap.stamprequest.service;

import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.employee.model.Employee;
import com.loyaltap.employee.model.EmployeeStatus;
import com.loyaltap.employee.repository.EmployeeRepository;
import com.loyaltap.membership.model.Membership;
import com.loyaltap.membership.model.MembershipStatus;
import com.loyaltap.membership.repository.MembershipRepository;
import com.loyaltap.nfc.model.NfcTag;
import com.loyaltap.nfc.model.NfcTagStatus;
import com.loyaltap.nfc.repository.NfcTagRepository;
import com.loyaltap.stamprequest.dto.CreateStampRequest;
import com.loyaltap.stamprequest.dto.EmployeeStampRequestAction;
import com.loyaltap.stamprequest.dto.StampRequestResponse;
import com.loyaltap.stamprequest.dto.UserStampRequestAction;
import com.loyaltap.stamprequest.mapper.StampRequestMapper;
import com.loyaltap.stamprequest.model.StampRequest;
import com.loyaltap.stamprequest.model.StampRequestStatus;
import com.loyaltap.stamprequest.repository.StampRequestRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class StampRequestServiceImpl implements StampRequestService {

    private static final Duration REQUEST_TTL = Duration.ofMinutes(5);

    private final StampRequestRepository stampRequestRepository;
    private final MembershipRepository membershipRepository;
    private final NfcTagRepository nfcTagRepository;
    private final EmployeeRepository employeeRepository;
    private final BusinessRepository businessRepository;
    private final StampRequestMapper stampRequestMapper;
    private final Clock clock;

    @Override
    @Transactional
    public StampRequestResponse createRequest(@NotBlank String tagCode, @Valid CreateStampRequest request) {
        StampRequest existing = stampRequestRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return replay(existing, tagCode, request);
        }

        Membership membership = membershipRepository.findById(request.membershipId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + request.membershipId()));
        requireMembershipUser(membership, request.userId());
        requireActiveMembership(membership);

        NfcTag tag = nfcTagRepository.findByTagCode(tagCode)
                .orElseThrow(() -> new ResourceNotFoundException("NFC tag not found: " + tagCode));
        if (tag.getStatus() != NfcTagStatus.ACTIVE) {
            throw new InvalidRequestException("NFC tag is not active: " + tagCode);
        }
        if (!tag.getBusiness().getId().equals(membership.getBusiness().getId())) {
            throw new InvalidRequestException("NFC tag and membership belong to different businesses");
        }

        Instant now = clock.instant();
        tag.setLastUsedAt(now);
        StampRequest stampRequest = stampRequestMapper.toEntity(
                request, membership, tag, now, now.plus(REQUEST_TTL));
        return stampRequestMapper.toResponse(stampRequestRepository.save(stampRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public StampRequestResponse getRequest(UUID requestId, @NotBlank String userId) {
        StampRequest request = findRequest(requestId);
        requireMembershipUser(request.getMembership(), userId);
        return stampRequestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public StampRequestResponse cancelRequest(UUID requestId, @Valid UserStampRequestAction action) {
        StampRequest request = findRequestForUpdate(requestId);
        requireMembershipUser(request.getMembership(), action.userId());
        if (request.getStatus() == StampRequestStatus.CANCELLED) {
            return stampRequestMapper.toResponse(request);
        }
        requirePending(request);
        complete(request, StampRequestStatus.CANCELLED);
        return stampRequestMapper.toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StampRequestResponse> listPendingRequests(UUID businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException("Business not found: " + businessId);
        }
        return stampRequestRepository.findAllByBusinessIdAndStatusOrderByCreatedAtAsc(
                        businessId, StampRequestStatus.PENDING)
                .stream()
                .map(stampRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StampRequestResponse approveRequest(UUID requestId, @Valid EmployeeStampRequestAction action) {
        StampRequest request = findRequestForUpdate(requestId);
        if (request.getStatus() == StampRequestStatus.APPROVED) {
            return stampRequestMapper.toResponse(request);
        }
        requirePendingAndUnexpired(request);
        Employee employee = findActiveEmployee(action.employeeId(), request.getBusiness().getId());
        Membership membership = membershipRepository.findByIdForUpdate(request.getMembership().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membership not found: " + request.getMembership().getId()));
        requireActiveMembership(membership);

        membership.setPointsBalance(membership.getPointsBalance() + request.getPointsToAdd());
        membership.setTotalPointsEarned(membership.getTotalPointsEarned() + request.getPointsToAdd());
        request.setMembership(membership);
        request.setApprovedByEmployee(employee);
        complete(request, StampRequestStatus.APPROVED);
        return stampRequestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public StampRequestResponse rejectRequest(UUID requestId, @Valid EmployeeStampRequestAction action) {
        StampRequest request = findRequestForUpdate(requestId);
        if (request.getStatus() == StampRequestStatus.REJECTED) {
            return stampRequestMapper.toResponse(request);
        }
        requirePendingAndUnexpired(request);
        findActiveEmployee(action.employeeId(), request.getBusiness().getId());
        complete(request, StampRequestStatus.REJECTED);
        return stampRequestMapper.toResponse(request);
    }

    @Override
    @Scheduled(fixedDelayString = "${loyaltap.stamp-request.expiry-delay-ms:60000}")
    @Transactional
    public int expirePendingRequests() {
        Instant now = clock.instant();
        int expired = 0;
        for (UUID requestId : stampRequestRepository.findIdsToExpire(StampRequestStatus.PENDING, now)) {
            StampRequest request = stampRequestRepository.findByIdForUpdate(requestId).orElse(null);
            if (request != null
                    && request.getStatus() == StampRequestStatus.PENDING
                    && !request.getExpiresAt().isAfter(now)) {
                request.setStatus(StampRequestStatus.EXPIRED);
                request.setProcessedAt(now);
                expired++;
            }
        }
        return expired;
    }

    private StampRequestResponse replay(StampRequest existing, String tagCode, CreateStampRequest request) {
        boolean sameRequest = existing.getMembership().getId().equals(request.membershipId())
                && existing.getMembership().getUser().getId().equals(request.userId())
                && existing.getNfcTag() != null
                && existing.getNfcTag().getTagCode().equals(tagCode);
        if (!sameRequest) {
            throw new DuplicateResourceException("Idempotency key is already used: " + request.idempotencyKey());
        }
        return stampRequestMapper.toResponse(existing);
    }

    private Employee findActiveEmployee(UUID employeeId, UUID businessId) {
        Employee employee = employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new InvalidRequestException("Employee is not active: " + employeeId);
        }
        return employee;
    }

    private StampRequest findRequest(UUID requestId) {
        return stampRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Stamp request not found: " + requestId));
    }

    private StampRequest findRequestForUpdate(UUID requestId) {
        return stampRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Stamp request not found: " + requestId));
    }

    private void requireMembershipUser(Membership membership, String userId) {
        if (!membership.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Membership not found: " + membership.getId());
        }
    }

    private void requireActiveMembership(Membership membership) {
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new InvalidRequestException("Membership is not active: " + membership.getId());
        }
    }

    private void requirePendingAndUnexpired(StampRequest request) {
        requirePending(request);
        if (!request.getExpiresAt().isAfter(clock.instant())) {
            throw new InvalidRequestException("Stamp request has expired: " + request.getId());
        }
    }

    private void requirePending(StampRequest request) {
        if (request.getStatus() != StampRequestStatus.PENDING) {
            throw new InvalidRequestException(
                    "Stamp request cannot transition from " + request.getStatus());
        }
    }

    private void complete(StampRequest request, StampRequestStatus status) {
        request.setStatus(status);
        request.setProcessedAt(clock.instant());
    }
}
