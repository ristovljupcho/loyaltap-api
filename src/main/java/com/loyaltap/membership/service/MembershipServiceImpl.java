package com.loyaltap.membership.service;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.membership.dto.CreateMembershipRequest;
import com.loyaltap.membership.dto.MembershipResponse;
import com.loyaltap.membership.mapper.MembershipMapper;
import com.loyaltap.membership.model.Membership;
import com.loyaltap.membership.model.MembershipStatus;
import com.loyaltap.membership.repository.MembershipRepository;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import com.loyaltap.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final MembershipMapper membershipMapper;

    @Override
    @Transactional
    public MembershipResponse createMembership(@Valid CreateMembershipRequest request) {
        User user = findActiveUser(request.userId());
        Business business = findActiveBusiness(request.businessId());
        if (membershipRepository.existsByUserIdAndBusinessId(user.getId(), business.getId())) {
            throw new DuplicateResourceException(
                    "Membership already exists for user " + user.getId() + " and business " + business.getId()
            );
        }

        Membership membership = membershipMapper.toEntity(user, business);
        return membershipMapper.toResponse(membershipRepository.save(membership));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponse> listActiveMemberships(@NotBlank String userId) {
        return membershipRepository.findAllByUserIdAndStatusOrderByCreatedAtAsc(userId, MembershipStatus.ACTIVE)
                .stream()
                .map(membershipMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipResponse getMembership(UUID membershipId) {
        return membershipMapper.toResponse(membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + membershipId)));
    }

    private User findActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRequestException("User is not active: " + userId);
        }
        return user;
    }

    private Business findActiveBusiness(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new InvalidRequestException("Business is not active: " + businessId);
        }
        return business;
    }
}
