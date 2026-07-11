package com.loyaltap.user.service;

import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.user.dto.CreateUserRequest;
import com.loyaltap.user.dto.UserResponse;
import com.loyaltap.user.mapper.UserMapper;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, new UserMapper());
    }

    @Test
    void createUserCreatesActiveUser() {
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(new CreateUserRequest("auth-user-1"));

        assertEquals("auth-user-1", response.id());
        assertEquals(UserStatus.ACTIVE, response.status());
    }

    @Test
    void createUserRejectsDuplicateId() {
        when(userRepository.existsById("auth-user-1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(new CreateUserRequest("auth-user-1")));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getUserReturnsExistingUser() {
        User user = user("auth-user-1", UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertEquals(user.getId(), userService.getUser(user.getId()).id());
    }

    @Test
    void getUserRejectsMissingUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUser("missing"));
    }

    @Test
    void listUsersReturnsMappedUsers() {
        when(userRepository.findAll()).thenReturn(List.of(
                user("first", UserStatus.ACTIVE),
                user("second", UserStatus.DISABLED)
        ));

        List<UserResponse> users = userService.listUsers();

        assertEquals(List.of("first", "second"), users.stream().map(UserResponse::id).toList());
    }

    @Test
    void deleteUserMarksUserDeleted() {
        User user = user("auth-user-1", UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponse response = userService.deleteUser(user.getId());

        assertEquals(UserStatus.DELETED, response.status());
    }

    @Test
    void deleteUserLeavesDeletedUserDeleted() {
        User user = user("auth-user-1", UserStatus.DELETED);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponse response = userService.deleteUser(user.getId());

        assertEquals(UserStatus.DELETED, response.status());
    }

    @Test
    void deleteUserRejectsMissingUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser("missing"));
    }

    private User user(String id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }
}
