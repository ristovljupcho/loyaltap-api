package com.loyaltap.user.service;

import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.user.dto.CreateUserRequest;
import com.loyaltap.user.dto.UserResponse;
import com.loyaltap.user.mapper.UserMapper;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import com.loyaltap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsById(request.id())) {
            throw new DuplicateResourceException("User already exists: " + request.id());
        }

        return userMapper.toResponse(userRepository.save(userMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(String userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse deleteUser(String userId) {
        User user = findUser(userId);
        if (user.getStatus() != UserStatus.DELETED) {
            user.setStatus(UserStatus.DELETED);
        }
        return userMapper.toResponse(user);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
