package com.loyaltap.reward.service;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.reward.dto.CreateRewardRequest;
import com.loyaltap.reward.dto.RewardResponse;
import com.loyaltap.reward.dto.UpdateRewardRequest;
import com.loyaltap.reward.mapper.RewardMapper;
import com.loyaltap.reward.model.Reward;
import com.loyaltap.reward.model.RewardStatus;
import com.loyaltap.reward.repository.RewardRepository;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import com.loyaltap.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final RewardRepository rewardRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final RewardMapper rewardMapper;

    @Override
    @Transactional
    public RewardResponse createReward(UUID businessId, @Valid CreateRewardRequest request) {
        Business business = findActiveBusiness(businessId);
        User user = findActiveUser(request.userId());
        String name = requireNonBlank(request.name(), "Reward name is required");

        Reward reward = rewardMapper.toEntity(request, business, user, name);
        return rewardMapper.toResponse(rewardRepository.save(reward));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardResponse> listActiveRewards(UUID businessId) {
        findActiveBusiness(businessId);
        return rewardRepository.findAllByBusinessIdAndStatusOrderByNameAsc(businessId, RewardStatus.ACTIVE)
                .stream()
                .map(rewardMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RewardResponse getReward(UUID rewardId) {
        return rewardMapper.toResponse(findReward(rewardId));
    }

    @Override
    @Transactional
    public RewardResponse updateReward(UUID rewardId, @Valid UpdateRewardRequest request) {
        Reward reward = findReward(rewardId);

        if (request.name() != null) {
            reward.setName(requireNonBlank(request.name(), "Reward name cannot be blank"));
        }
        if (request.description() != null) {
            reward.setDescription(trimToNull(request.description()));
        }
        if (request.requiredPoints() != null) {
            reward.setRequiredPoints(request.requiredPoints());
        }
        // ponytail: null means "unchanged"; add an explicit unlimited flag when that transition is needed.
        if (request.stockQuantity() != null) {
            validateStockQuantity(reward, request.stockQuantity());
            reward.setStockQuantity(request.stockQuantity());
        }

        return rewardMapper.toResponse(reward);
    }

    @Override
    @Transactional
    public void deleteReward(UUID rewardId) {
        Reward reward = findRewardById(rewardId);
        if (reward.getStatus() != RewardStatus.DELETED) {
            reward.setStatus(RewardStatus.DELETED);
        }
    }

    private Business findActiveBusiness(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new InvalidRequestException("Business is not active: " + businessId);
        }
        return business;
    }

    private User findActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRequestException("User is not active: " + userId);
        }
        return user;
    }

    private Reward findReward(UUID rewardId) {
        Reward reward = findRewardById(rewardId);
        if (reward.getStatus() == RewardStatus.DELETED) {
            throw new ResourceNotFoundException("Reward not found: " + rewardId);
        }
        return reward;
    }

    private Reward findRewardById(UUID rewardId) {
        return rewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found: " + rewardId));
    }

    private void validateStockQuantity(Reward reward, int stockQuantity) {
        int committedStock = reward.getStockReserved() + reward.getStockRedeemed();
        if (stockQuantity < committedStock) {
            throw new InvalidRequestException("Stock quantity cannot be lower than reserved and redeemed stock");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidRequestException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
