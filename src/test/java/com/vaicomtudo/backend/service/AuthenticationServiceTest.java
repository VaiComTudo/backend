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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;

import com.vaicomtudo.backend.auth.AuthenticationRequest;
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

    @Mock
    private AuthenticationManager authenticationManager;

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

    @Test
    @Requirement("VCT-81")
    @DisplayName("Test successful authentication")
    void testSuccessfulAuthentication() {
        AuthenticationRequest request = AuthenticationRequest.builder()
            .email("test@email.com")
            .password("pass")
            .build();

        Account account = Account.builder()
            .email("test@email.com")
            .passwordHash("encoded")
            .name("Test User")
            .build();

        User user = User.builder()
            .account(account)
            .role(Role.NORMAL_USER)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        when(userRepository.findByAccountEmail("test@email.com"))
            .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any());
        verify(userRepository).findByAccountEmail("test@email.com");
        verify(jwtService).generateToken(user);
    }

    @Test
    @Requirement("VCT-81")
    @DisplayName("Test authentication with invalid credentials")
    void testAuthenticationWithInvalidCredentials() {
        AuthenticationRequest request = AuthenticationRequest.builder()
            .email("test@email.com")
            .password("wrongpass")
            .build();

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
            () -> authenticationService.authenticate(request));

        verify(authenticationManager).authenticate(any());
        verify(userRepository, never()).findByAccountEmail(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @Requirement("VCT-81")
    @DisplayName("Test authentication with non-existent user")
    void testAuthenticationWithNonExistentUser() {
        AuthenticationRequest request = AuthenticationRequest.builder()
            .email("nonexistent@email.com")
            .password("pass")
            .build();

        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        when(userRepository.findByAccountEmail("nonexistent@email.com"))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> authenticationService.authenticate(request));

        verify(authenticationManager).authenticate(any());
        verify(userRepository).findByAccountEmail("nonexistent@email.com");
        verify(jwtService, never()).generateToken(any());
    }
}
