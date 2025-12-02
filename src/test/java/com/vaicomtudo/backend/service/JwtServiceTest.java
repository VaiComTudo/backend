package com.vaicomtudo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;

public class JwtServiceTest {
    
    @Test
    @DisplayName("Test token generation and validation.")
    void testGenerateAndValidateToken() {
        String secretKey = "dGhpc2lzYXNlY3JldGtleWZvcnRlc3RpbmcxMjM0abcd5678";
        JwtService service = new JwtService(secretKey);
        
        // Create account
        Account account = new Account();
        account.setName("name");
        account.setEmail("test@email.com");
        account.setPasswordHash("hash");

        // Create user
        User user = User.builder()
            .account(account)
            .birthdate(LocalDate.of(1990, 1, 1))
            .rating(0.0)
            .role(Role.NORMAL_USER)
            .build();

        String token = service.generateToken(user);
        assertThat(service.isTokenValid(token, user)).isTrue();
        assertThat(service.getUsername(token)).isEqualTo("test@email.com");
    }
}
