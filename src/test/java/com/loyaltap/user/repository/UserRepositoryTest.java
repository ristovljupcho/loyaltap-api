package com.loyaltap.user.repository;

import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsStringIdStatusAndAuditTimestamps() {
        User user = new User();
        user.setId("auth-user-1");

        User savedUser = userRepository.saveAndFlush(user);

        assertEquals("auth-user-1", savedUser.getId());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }
}
