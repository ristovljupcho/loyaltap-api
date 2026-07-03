package com.loyaltap.reward.service;

import com.loyaltap.reward.dto.CreateRewardRequest;
import com.loyaltap.reward.dto.RewardResponse;
import com.loyaltap.reward.dto.UpdateRewardRequest;

import java.util.List;
import java.util.UUID;

public interface RewardService {

    RewardResponse createReward(UUID businessId, CreateRewardRequest request);

    List<RewardResponse> listActiveRewards(UUID businessId);

    RewardResponse getReward(UUID rewardId);

    RewardResponse updateReward(UUID rewardId, UpdateRewardRequest request);

    void deleteReward(UUID rewardId);
}
