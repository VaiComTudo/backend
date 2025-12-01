package com.vaicomtudo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaicomtudo.backend.auth.AuthenticationResponse;
import com.vaicomtudo.backend.auth.RegisterRequest;
import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.UserRepository;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
    
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @Requirement("VCT-80")
    @DisplayName("Test user register.")
    void testRegister() {
        RegisterRequest request = RegisterRequest.builder()
            .name("Name")
            .email("test@email.com")
            .password("pass")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
        
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(userRepository.findByAccountEmail("test@email.com"))
            .thenReturn(Optional.empty());
        
        AuthenticationResponse response = authenticationService.register(request, Role.NORMAL_USER);
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    @Requirement("VCT-80")
    @DisplayName("Test register duplicate email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
            .name("Name")
            .email("test@email.com")
            .password("pass")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
        
        Account account = Account.builder().email("test@email.com").build();
        
        when(userRepository.findByAccountEmail("test@email.com"))
            .thenReturn(Optional.of(User.builder().account(account).build()));
        
        Exception exception = assertThrows(IllegalStateException.class, () -> authenticationService.register(request, Role.NORMAL_USER));
        assertThat(exception.getMessage()).isEqualTo("Email already registered.");
        verify(userRepository, never()).save(any());
    }

    @Test
    @Requirement("VCT-80")
    @DisplayName("Test register user under 18")
    void testRegisterUserUnderLegalAge() {
        LocalDate birthdate = LocalDate.now().minusYears(10);

        RegisterRequest request = RegisterRequest.builder()
            .name("Young User")
            .email("young@email.com")
            .password("pass")
            .birthdate(birthdate)
            .build();

        when(userRepository.findByAccountEmail("young@email.com"))
            .thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class,
            () -> authenticationService.register(request, Role.NORMAL_USER));

        assertThat(exception.getMessage()).isEqualTo("User must be at least 18 years old.");
        verify(userRepository, never()).save(any());
    }

    @Test
    @Requirement("VCT-80")
    @DisplayName("Test register user exactly 18 years old")
    void testRegisterUserExactly18() {
        LocalDate birthdate = LocalDate.now().minusYears(18);

        RegisterRequest request = RegisterRequest.builder()
            .name("Name")
            .email("adult@email.com")
            .password("pass")
            .birthdate(birthdate)
            .build();

        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(userRepository.findByAccountEmail("adult@email.com"))
            .thenReturn(Optional.empty());

        AuthenticationResponse response = authenticationService.register(request, Role.NORMAL_USER);
        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(userRepository).save(any(User.class));
    }
}
