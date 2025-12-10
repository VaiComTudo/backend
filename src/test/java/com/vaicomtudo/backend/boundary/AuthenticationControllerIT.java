package com.vaicomtudo.backend.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.vaicomtudo.backend.auth.AuthenticationRequest;
import com.vaicomtudo.backend.auth.RegisterRequest;
import com.vaicomtudo.backend.config.AbstractIntegrationTest;
import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.AccountRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;


class AuthenticationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration test: Successful user registration")
    @Requirement("VCT-80")
    void whenRegisterWithValidData_thenReturns201AndJwtToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .name("John Doe")
            .email("john.doe@test.com")
            .password("securePassword123")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .param("role", "NORMAL_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.token").isNotEmpty());

        // Verify user was saved in database
        var user = userRepository.findByAccountEmail("john.doe@test.com");
        assertThat(user).isPresent();
        assertThat(user.get().getAccount().getName()).isEqualTo("John Doe");
        assertThat(user.get().getAccount().getEmail()).isEqualTo("john.doe@test.com");
        assertThat(user.get().getBirthdate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(user.get().getRole()).isEqualTo(Role.NORMAL_USER);

        // Verify password is encoded (not plain text)
        assertThat(user.get().getAccount().getPasswordHash()).isNotEqualTo("securePassword123");
        assertThat(passwordEncoder.matches("securePassword123", user.get().getAccount().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Integration test: Register with duplicate email")
    @Requirement("VCT-80")
    void whenRegisterWithDuplicateEmail_thenReturns400() throws Exception {
        // Create existing user in database
        Account account = new Account();
        account.setName("Existing User");
        account.setEmail("existing@test.com");
        account.setPasswordHash(passwordEncoder.encode("password"));
        account = accountRepository.save(account);

        User user = new User();
        user.setAccount(account);
        user.setBirthdate(LocalDate.of(1990, 1, 1));
        user.setRating(0.0);
        user.setRole(Role.NORMAL_USER);
        userRepository.save(user);

        // Try to register with same email
        RegisterRequest request = RegisterRequest.builder()
            .name("New User")
            .email("existing@test.com")
            .password("password123")
            .birthdate(LocalDate.of(1995, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .param("role", "NORMAL_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        // Verify only one user exists
        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Integration test: Register user under 18 years old")
    @Requirement("VCT-80")
    void whenRegisterUnder18_thenReturns400() throws Exception {
        LocalDate under18Birthdate = LocalDate.now().minusYears(17);

        RegisterRequest request = RegisterRequest.builder()
            .name("Young User")
            .email("young@test.com")
            .password("password123")
            .birthdate(under18Birthdate)
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .param("role", "NORMAL_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        // Verify user was not saved
        assertThat(userRepository.findByAccountEmail("young@test.com")).isEmpty();
    }

    @Test
    @DisplayName("Integration test: Register user exactly 18 years old")
    @Requirement("VCT-80")
    void whenRegisterExactly18_thenReturns201() throws Exception {
        LocalDate exactly18Birthdate = LocalDate.now().minusYears(18);

        RegisterRequest request = RegisterRequest.builder()
            .name("Adult User")
            .email("adult@test.com")
            .password("password123")
            .birthdate(exactly18Birthdate)
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .param("role", "NORMAL_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());

        // Verify user was saved
        assertThat(userRepository.findByAccountEmail("adult@test.com")).isPresent();
    }

    @Test
    @DisplayName("Integration test: Successful login with correct credentials")
    @Requirement("VCT-81")
    void whenLoginWithCorrectCredentials_thenReturns200AndJwtToken() throws Exception {
        // Create user in database
        Account account = new Account();
        account.setName("Test User");
        account.setEmail("testuser@test.com");
        account.setPasswordHash(passwordEncoder.encode("correctPassword"));
        account = accountRepository.save(account);

        User user = new User();
        user.setAccount(account);
        user.setBirthdate(LocalDate.of(1990, 1, 1));
        user.setRating(0.0);
        user.setRole(Role.NORMAL_USER);
        userRepository.save(user);

        // Login with correct credentials
        AuthenticationRequest request = AuthenticationRequest.builder()
            .email("testuser@test.com")
            .password("correctPassword")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Integration test: Login with incorrect password")
    @Requirement("VCT-81")
    void whenLoginWithIncorrectPassword_thenReturns403() throws Exception {
        // Create user in database
        Account account = new Account();
        account.setName("Test User");
        account.setEmail("testuser2@test.com");
        account.setPasswordHash(passwordEncoder.encode("correctPassword"));
        account = accountRepository.save(account);

        User user = new User();
        user.setAccount(account);
        user.setBirthdate(LocalDate.of(1990, 1, 1));
        user.setRating(0.0);
        user.setRole(Role.NORMAL_USER);
        userRepository.save(user);

        // Login with incorrect password
        AuthenticationRequest request = AuthenticationRequest.builder()
            .email("testuser2@test.com")
            .password("wrongPassword")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: Login with non-existent user")
    @Requirement("VCT-81")
    void whenLoginWithNonExistentUser_thenReturns403() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
            .email("nonexistent@test.com")
            .password("password123")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Integration test: Register and immediately login")
    @Requirement("VCT-80")
    void whenRegisterThenLogin_thenBothSucceed() throws Exception {
        // Register new user
        RegisterRequest registerRequest = RegisterRequest.builder()
            .name("New User")
            .email("newuser@test.com")
            .password("myPassword123")
            .birthdate(LocalDate.of(1995, 6, 15))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .param("role", "NORMAL_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());

        // Immediately login with same credentials
        AuthenticationRequest loginRequest = AuthenticationRequest.builder()
            .email("newuser@test.com")
            .password("myPassword123")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }
}
