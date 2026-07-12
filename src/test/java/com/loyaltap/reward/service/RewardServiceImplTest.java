package com.loyaltap.reward.service;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.reward.dto.CreateRewardRequest;
import com.loyaltap.reward.dto.RewardResponse;
import com.loyaltap.reward.mapper.RewardMapper;
import com.loyaltap.reward.model.Reward;
import com.loyaltap.reward.repository.RewardRepository;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceImplTest {

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private UserRepository userRepository;

    private RewardServiceImpl rewardService;

    @BeforeEach
    void setUp() {
        rewardService = new RewardServiceImpl(
                rewardRepository,
                businessRepository,
                userRepository,
                new RewardMapper()
        );
    }

    @Test
    void createRewardLinksActiveUser() {
        Business business = activeBusiness();
        User user = user(UserStatus.ACTIVE);
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(rewardRepository.save(any(Reward.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardResponse response = rewardService.createReward(business.getId(), request(user.getId()));

        assertEquals(user.getId(), response.userId());
        verify(rewardRepository).save(any(Reward.class));
    }

    @Test
    void createRewardRejectsMissingUser() {
        Business business = activeBusiness();
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> rewardService.createReward(business.getId(), request("missing")));
        verify(rewardRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"DISABLED", "DELETED"})
    void createRewardRejectsInactiveUser(UserStatus status) {
        Business business = activeBusiness();
        User user = user(status);
        when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(InvalidRequestException.class,
                () -> rewardService.createReward(business.getId(), request(user.getId())));
        verify(rewardRepository, never()).save(any());
    }

    private Business activeBusiness() {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setStatus(BusinessStatus.ACTIVE);
        return business;
    }

    private User user(UserStatus status) {
        User user = new User();
        user.setId("user-1");
        user.setStatus(status);
        return user;
    }

    private CreateRewardRequest request(String userId) {
        return new CreateRewardRequest(userId, "Free coffee", null, 10, null);
    }
}
