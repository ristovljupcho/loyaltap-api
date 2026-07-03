package com.loyaltap.reward.repository;

import com.loyaltap.reward.model.Reward;
import com.loyaltap.reward.model.RewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardRepository extends JpaRepository<Reward, UUID> {

    List<Reward> findAllByBusinessIdAndStatusOrderByNameAsc(UUID businessId, RewardStatus status);
}
