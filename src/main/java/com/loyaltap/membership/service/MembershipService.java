package com.loyaltap.membership.service;

import com.loyaltap.membership.dto.CreateMembershipRequest;
import com.loyaltap.membership.dto.MembershipResponse;

import java.util.List;
import java.util.UUID;

public interface MembershipService {

    MembershipResponse createMembership(CreateMembershipRequest request);

    List<MembershipResponse> listActiveMemberships(String userId);

    MembershipResponse getMembership(UUID membershipId);
}
