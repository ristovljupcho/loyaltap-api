package com.loyaltap.reward.controller;

import com.loyaltap.reward.dto.CreateRewardRequest;
import com.loyaltap.reward.dto.RewardResponse;
import com.loyaltap.reward.dto.UpdateRewardRequest;
import com.loyaltap.reward.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @PostMapping("/business/{businessId}/rewards")
    public ResponseEntity<RewardResponse> createReward(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreateRewardRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rewardService.createReward(businessId, request));
    }

    @GetMapping("/business/{businessId}/rewards")
    public List<RewardResponse> listRewards(@PathVariable UUID businessId) {
        return rewardService.listActiveRewards(businessId);
    }

    @GetMapping("/rewards/{rewardId}")
    public RewardResponse getReward(@PathVariable UUID rewardId) {
        return rewardService.getReward(rewardId);
    }

    @PatchMapping("/rewards/{rewardId}")
    public RewardResponse updateReward(
            @PathVariable UUID rewardId,
            @Valid @RequestBody UpdateRewardRequest request
    ) {
        return rewardService.updateReward(rewardId, request);
    }

    @DeleteMapping("/rewards/{rewardId}")
    public ResponseEntity<Void> deleteReward(@PathVariable UUID rewardId) {
        rewardService.deleteReward(rewardId);
        return ResponseEntity.noContent().build();
    }
}
