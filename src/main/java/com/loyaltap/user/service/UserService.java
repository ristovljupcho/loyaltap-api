package com.loyaltap.user.service;

import com.loyaltap.user.dto.CreateUserRequest;
import com.loyaltap.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public interface UserService {

    UserResponse createUser(@Valid CreateUserRequest request);

    UserResponse getUser(@NotBlank String userId);

    List<UserResponse> listUsers();

    UserResponse deleteUser(@NotBlank String userId);
}
