package com.loyaltap.reward.utils;

import com.loyaltap.reward.model.Reward;

public final class RewardStockUtils {

    private RewardStockUtils() {
    }

    public static Integer availableStock(Reward reward) {
        return reward.getStockQuantity() == null
                ? null
                : reward.getStockQuantity() - reward.getStockReserved() - reward.getStockRedeemed();
    }
}
