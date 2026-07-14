package com.loyaltap.stamprequest.service;

import com.loyaltap.business.model.Business;
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
import com.loyaltap.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StampRequestServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private StampRequestRepository stampRequestRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private NfcTagRepository nfcTagRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BusinessRepository businessRepository;

    private StampRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StampRequestServiceImpl(
                stampRequestRepository,
                membershipRepository,
                nfcTagRepository,
                employeeRepository,
                businessRepository,
                new StampRequestMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsPendingRequestAndRecordsTagUsage() {
        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        NfcTag tag = tag(membership.getBusiness(), NfcTagStatus.ACTIVE);
        CreateStampRequest create = new CreateStampRequest(membership.getId(), "user-1", "key-1");
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));
        when(nfcTagRepository.findByTagCode(tag.getTagCode())).thenReturn(Optional.of(tag));
        when(stampRequestRepository.save(any(StampRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StampRequestResponse response = service.createRequest(tag.getTagCode(), create);

        assertEquals(StampRequestStatus.PENDING, response.status());
        assertEquals(1, response.pointsToAdd());
        assertEquals(NOW.plusSeconds(300), response.expiresAt());
        assertEquals(NOW, tag.getLastUsedAt());
    }

    @Test
    void replaysSameIdempotencyKeyAndRejectsConflictingReuse() {
        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        NfcTag tag = tag(membership.getBusiness(), NfcTagStatus.ACTIVE);
        StampRequest existing = request(membership, tag, StampRequestStatus.PENDING);
        existing.setIdempotencyKey("key-1");
        when(stampRequestRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        StampRequestResponse replay = service.createRequest(
                tag.getTagCode(), new CreateStampRequest(membership.getId(), "user-1", "key-1"));
        assertEquals(existing.getId(), replay.id());

        assertThrows(DuplicateResourceException.class, () -> service.createRequest(
                tag.getTagCode(), new CreateStampRequest(UUID.randomUUID(), "user-1", "key-1")));
        verify(stampRequestRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveMembershipTagAndCrossBusinessTag() {
        Membership inactive = membership("user-1", MembershipStatus.BLOCKED);
        when(membershipRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));
        assertThrows(InvalidRequestException.class, () -> service.createRequest(
                "tag", new CreateStampRequest(inactive.getId(), "user-1", "key-1")));

        Membership active = membership("user-1", MembershipStatus.ACTIVE);
        when(membershipRepository.findById(active.getId())).thenReturn(Optional.of(active));
        NfcTag disabled = tag(active.getBusiness(), NfcTagStatus.DISABLED);
        when(nfcTagRepository.findByTagCode(disabled.getTagCode())).thenReturn(Optional.of(disabled));
        assertThrows(InvalidRequestException.class, () -> service.createRequest(
                disabled.getTagCode(), new CreateStampRequest(active.getId(), "user-1", "key-2")));

        NfcTag otherBusiness = tag(business(), NfcTagStatus.ACTIVE);
        when(nfcTagRepository.findByTagCode(otherBusiness.getTagCode())).thenReturn(Optional.of(otherBusiness));
        assertThrows(InvalidRequestException.class, () -> service.createRequest(
                otherBusiness.getTagCode(), new CreateStampRequest(active.getId(), "user-1", "key-3")));
    }

    @Test
    void approvalLocksRowsAndAddsPointsExactlyOnce() {
        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        membership.setPointsBalance(4);
        membership.setTotalPointsEarned(4);
        NfcTag tag = tag(membership.getBusiness(), NfcTagStatus.ACTIVE);
        StampRequest request = request(membership, tag, StampRequestStatus.PENDING);
        Employee employee = employee(membership.getBusiness(), EmployeeStatus.ACTIVE);
        when(stampRequestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(employeeRepository.findByIdAndBusinessId(employee.getId(), membership.getBusiness().getId()))
                .thenReturn(Optional.of(employee));
        when(membershipRepository.findByIdForUpdate(membership.getId())).thenReturn(Optional.of(membership));

        StampRequestResponse approved = service.approveRequest(
                request.getId(), new EmployeeStampRequestAction(employee.getId()));

        assertEquals(StampRequestStatus.APPROVED, approved.status());
        assertEquals(employee.getId(), approved.approvedByEmployeeId());
        assertEquals(5, membership.getPointsBalance());
        assertEquals(5, membership.getTotalPointsEarned());

        service.approveRequest(request.getId(), new EmployeeStampRequestAction(employee.getId()));
        assertEquals(5, membership.getPointsBalance());
        verify(membershipRepository).findByIdForUpdate(membership.getId());
    }

    @Test
    void approvalRejectsExpiredOrWrongBusinessEmployee() {
        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        StampRequest expired = request(membership, tag(membership.getBusiness(), NfcTagStatus.ACTIVE),
                StampRequestStatus.PENDING);
        expired.setExpiresAt(NOW);
        when(stampRequestRepository.findByIdForUpdate(expired.getId())).thenReturn(Optional.of(expired));
        assertThrows(InvalidRequestException.class, () -> service.approveRequest(
                expired.getId(), new EmployeeStampRequestAction(UUID.randomUUID())));

        StampRequest pending = request(membership, tag(membership.getBusiness(), NfcTagStatus.ACTIVE),
                StampRequestStatus.PENDING);
        UUID employeeId = UUID.randomUUID();
        when(stampRequestRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        when(employeeRepository.findByIdAndBusinessId(employeeId, membership.getBusiness().getId()))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.approveRequest(
                pending.getId(), new EmployeeStampRequestAction(employeeId)));
        verify(membershipRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void customerAndEmployeeTerminalTransitionsAreIdempotentOnlyForSameState() {
        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        NfcTag tag = tag(membership.getBusiness(), NfcTagStatus.ACTIVE);
        StampRequest cancelled = request(membership, tag, StampRequestStatus.PENDING);
        when(stampRequestRepository.findByIdForUpdate(cancelled.getId())).thenReturn(Optional.of(cancelled));

        service.cancelRequest(cancelled.getId(), new UserStampRequestAction("user-1"));
        assertEquals(StampRequestStatus.CANCELLED,
                service.cancelRequest(cancelled.getId(), new UserStampRequestAction("user-1")).status());
        assertThrows(InvalidRequestException.class, () -> service.rejectRequest(
                cancelled.getId(), new EmployeeStampRequestAction(UUID.randomUUID())));

        StampRequest rejected = request(membership, tag, StampRequestStatus.PENDING);
        Employee employee = employee(membership.getBusiness(), EmployeeStatus.ACTIVE);
        when(stampRequestRepository.findByIdForUpdate(rejected.getId())).thenReturn(Optional.of(rejected));
        when(employeeRepository.findByIdAndBusinessId(employee.getId(), membership.getBusiness().getId()))
                .thenReturn(Optional.of(employee));
        service.rejectRequest(rejected.getId(), new EmployeeStampRequestAction(employee.getId()));
        assertEquals(StampRequestStatus.REJECTED,
                service.rejectRequest(rejected.getId(), new EmployeeStampRequestAction(employee.getId())).status());
    }

    @Test
    void expiresOnlyDuePendingRequests() {
        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        NfcTag tag = tag(membership.getBusiness(), NfcTagStatus.ACTIVE);
        StampRequest due = request(membership, tag, StampRequestStatus.PENDING);
        due.setExpiresAt(NOW);
        StampRequest alreadyApproved = request(membership, tag, StampRequestStatus.APPROVED);
        when(stampRequestRepository.findIdsToExpire(StampRequestStatus.PENDING, NOW))
                .thenReturn(List.of(due.getId(), alreadyApproved.getId(), UUID.randomUUID()));
        when(stampRequestRepository.findByIdForUpdate(due.getId())).thenReturn(Optional.of(due));
        when(stampRequestRepository.findByIdForUpdate(alreadyApproved.getId())).thenReturn(Optional.of(alreadyApproved));

        assertEquals(1, service.expirePendingRequests());
        assertEquals(StampRequestStatus.EXPIRED, due.getStatus());
        assertEquals(NOW, due.getProcessedAt());
        assertEquals(StampRequestStatus.APPROVED, alreadyApproved.getStatus());
    }

    @Test
    void listValidatesBusinessAndGetHidesOtherUsersMembership() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.existsById(businessId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.listPendingRequests(businessId));

        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        StampRequest request = request(membership, tag(membership.getBusiness(), NfcTagStatus.ACTIVE),
                StampRequestStatus.PENDING);
        when(stampRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        assertThrows(ResourceNotFoundException.class, () -> service.getRequest(request.getId(), "user-2"));
    }

    private Membership membership(String userId, MembershipStatus status) {
        User user = new User();
        user.setId(userId);
        Membership membership = new Membership();
        membership.setId(UUID.randomUUID());
        membership.setUser(user);
        membership.setBusiness(business());
        membership.setStatus(status);
        return membership;
    }

    private Business business() {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        return business;
    }

    private NfcTag tag(Business business, NfcTagStatus status) {
        NfcTag tag = new NfcTag();
        tag.setId(UUID.randomUUID());
        tag.setBusiness(business);
        tag.setTagCode(UUID.randomUUID().toString());
        tag.setStatus(status);
        return tag;
    }

    private Employee employee(Business business, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setBusiness(business);
        employee.setStatus(status);
        return employee;
    }

    private StampRequest request(Membership membership, NfcTag tag, StampRequestStatus status) {
        StampRequest request = new StampRequest();
        request.setId(UUID.randomUUID());
        request.setMembership(membership);
        request.setBusiness(membership.getBusiness());
        request.setNfcTag(tag);
        request.setPointsToAdd(1);
        request.setStatus(status);
        request.setIdempotencyKey(UUID.randomUUID().toString());
        request.setCreatedAt(NOW.minusSeconds(60));
        request.setExpiresAt(NOW.plusSeconds(240));
        return request;
    }
}
