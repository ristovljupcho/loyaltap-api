package com.loyaltap.membership.mapper;

import com.loyaltap.business.model.Business;
import com.loyaltap.membership.dto.MembershipResponse;
import com.loyaltap.membership.model.Membership;
import com.loyaltap.membership.model.MembershipStatus;
import com.loyaltap.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class MembershipMapper {

    public Membership toEntity(User user, Business business) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setBusiness(business);
        membership.setStatus(MembershipStatus.ACTIVE);
        return membership;
    }

    public MembershipResponse toResponse(Membership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getBusiness().getId(),
                membership.getPointsBalance(),
                membership.getReservedPoints(),
                membership.getTotalPointsEarned(),
                membership.getTotalPointsSpent(),
                membership.getTotalRewardsRedeemed(),
                membership.getStatus(),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
        );
    }
}
