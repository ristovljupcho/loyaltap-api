package com.loyaltap.user.mapper;

import com.loyaltap.user.dto.CreateUserRequest;
import com.loyaltap.user.dto.UserResponse;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        User user = new User();
        user.setId(request.id());
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
