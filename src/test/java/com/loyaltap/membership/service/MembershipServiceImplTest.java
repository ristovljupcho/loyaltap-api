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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessRepository businessRepository;

    private MembershipServiceImpl membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipServiceImpl(
                membershipRepository,
                userRepository,
                businessRepository,
                new MembershipMapper()
        );
    }

    @Test
    void createsActiveMembershipWithServerControlledBalances() {
        User user = user("user-1", UserStatus.ACTIVE);
        Business business = business(BusinessStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MembershipResponse response = membershipService.createMembership(
                new CreateMembershipRequest(user.getId(), business.getId())
        );

        assertEquals(user.getId(), response.userId());
        assertEquals(business.getId(), response.businessId());
        assertEquals(MembershipStatus.ACTIVE, response.status());
        assertEquals(0, response.pointsBalance());
        assertEquals(0, response.reservedPoints());
        assertEquals(0, response.totalPointsEarned());
        assertEquals(0, response.totalPointsSpent());
        assertEquals(0, response.totalRewardsRedeemed());
    }

    @Test
    void createRejectsMissingOrInactiveUserWithoutLoadingBusiness() {
        Business business = business(BusinessStatus.ACTIVE);
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        when(userRepository.findById("disabled")).thenReturn(Optional.of(user("disabled", UserStatus.DISABLED)));

        assertThrows(ResourceNotFoundException.class, () -> membershipService.createMembership(
                new CreateMembershipRequest("missing", business.getId())
        ));
        assertThrows(InvalidRequestException.class, () -> membershipService.createMembership(
                new CreateMembershipRequest("disabled", business.getId())
        ));
        verify(businessRepository, never()).findById(any());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingOrInactiveBusinessWithoutSaving() {
        User user = user("user-1", UserStatus.ACTIVE);
        UUID missingBusinessId = UUID.randomUUID();
        Business inactiveBusiness = business(BusinessStatus.INACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(businessRepository.findById(missingBusinessId)).thenReturn(Optional.empty());
        when(businessRepository.findById(inactiveBusiness.getId())).thenReturn(Optional.of(inactiveBusiness));

        assertThrows(ResourceNotFoundException.class, () -> membershipService.createMembership(
                new CreateMembershipRequest(user.getId(), missingBusinessId)
        ));
        assertThrows(InvalidRequestException.class, () -> membershipService.createMembership(
                new CreateMembershipRequest(user.getId(), inactiveBusiness.getId())
        ));
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateAssignmentIncludingInactiveMemberships() {
        User user = user("user-1", UserStatus.ACTIVE);
        Business business = business(BusinessStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(membershipRepository.existsByUserIdAndBusinessId(user.getId(), business.getId())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> membershipService.createMembership(
                new CreateMembershipRequest(user.getId(), business.getId())
        ));
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void listsOnlyActiveMembershipsForUser() {
        Membership membership = membership("user-1", MembershipStatus.ACTIVE);
        when(membershipRepository.findAllByUserIdAndStatusOrderByCreatedAtAsc(
                "user-1", MembershipStatus.ACTIVE
        )).thenReturn(List.of(membership));

        List<MembershipResponse> responses = membershipService.listActiveMemberships("user-1");

        assertEquals(1, responses.size());
        assertEquals(MembershipStatus.ACTIVE, responses.getFirst().status());
    }

    @Test
    void getsMembershipOrReportsMissingResource() {
        Membership membership = membership("user-1", MembershipStatus.BLOCKED);
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));

        assertEquals(MembershipStatus.BLOCKED, membershipService.getMembership(membership.getId()).status());

        UUID missingId = UUID.randomUUID();
        when(membershipRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> membershipService.getMembership(missingId));
    }

    private User user(String id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private Business business(BusinessStatus status) {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setStatus(status);
        return business;
    }

    private Membership membership(String userId, MembershipStatus status) {
        Membership membership = new Membership();
        membership.setId(UUID.randomUUID());
        membership.setUser(user(userId, UserStatus.ACTIVE));
        membership.setBusiness(business(BusinessStatus.ACTIVE));
        membership.setStatus(status);
        return membership;
    }
}
