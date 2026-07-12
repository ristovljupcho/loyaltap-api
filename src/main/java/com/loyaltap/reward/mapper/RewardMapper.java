package com.loyaltap.reward.mapper;

import com.loyaltap.business.model.Business;
import com.loyaltap.reward.dto.CreateRewardRequest;
import com.loyaltap.reward.dto.RewardResponse;
import com.loyaltap.reward.model.Reward;
import com.loyaltap.reward.model.RewardStatus;
import com.loyaltap.reward.utils.RewardStockUtils;
import com.loyaltap.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class RewardMapper {

    public Reward toEntity(CreateRewardRequest request, Business business, User user, String name) {
        Reward reward = new Reward();
        reward.setBusiness(business);
        reward.setUser(user);
        reward.setName(name);
        reward.setDescription(trimToNull(request.description()));
        reward.setRequiredPoints(request.requiredPoints());
        reward.setStockQuantity(request.stockQuantity());
        reward.setStatus(RewardStatus.ACTIVE);
        return reward;
    }

    public RewardResponse toResponse(Reward reward) {
        return new RewardResponse(
                reward.getId(),
                reward.getBusiness().getId(),
                reward.getUser().getId(),
                reward.getName(),
                reward.getDescription(),
                reward.getRequiredPoints(),
                reward.getStockQuantity(),
                reward.getStockReserved(),
                reward.getStockRedeemed(),
                RewardStockUtils.availableStock(reward),
                reward.getStatus()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
