package com.vaicomtudo.backend.boundary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.vaicomtudo.backend.auth.AuthenticationRequest;
import com.vaicomtudo.backend.auth.RegisterRequest;
import com.vaicomtudo.backend.data.repository.UserRepository;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Test /register endpoint with valid data")
    @Requirement("VCT-80")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .name("Test User")
            .email("test@email.com")
            .password("password123")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Test /register endpoint with duplicate email")
    @Requirement("VCT-80")
    void testRegisterDuplicateEmail() throws Exception {
        // First registration
        RegisterRequest request = RegisterRequest.builder()
            .name("Test User")
            .email("duplicate@email.com")
            .password("password123")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        // Second registration with same email
        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.String").value("Email already registered."));
    }

    @Test
    @DisplayName("Test /register endpoint with user under 18")
    @Requirement("VCT-80")
    void testRegisterUserUnder18() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .name("Young User")
            .email("young@email.com")
            .password("password123")
            .birthdate(LocalDate.now().minusYears(10))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.String").value("User must be at least 18 years old."));
    }

    @Test
    @DisplayName("Test /register endpoint with user exactly 18 years old")
    @Requirement("VCT-80")
    void testRegisterUserExactly18() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .name("Adult User")
            .email("adult@email.com")
            .password("password123")
            .birthdate(LocalDate.now().minusYears(18))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Test /register endpoint with invalid JSON")
    @Requirement("VCT-80")
    void testRegisterInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test /register endpoint with missing role parameter")
    @Requirement("VCT-80")
    void testRegisterMissingRole() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .name("Test User")
            .email("test@email.com")
            .password("password123")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test /register endpoint with ADMIN role")
    @Requirement("VCT-80")
    void testRegisterAdminRole() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .name("Admin User")
            .email("admin@email.com")
            .password("password123")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "ADMIN")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Test /login endpoint with valid credentials")
    @Requirement("VCT-81")
    void testLoginSuccess() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
            .name("Test User")
            .email("login@email.com")
            .password("password123")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        AuthenticationRequest loginRequest = AuthenticationRequest.builder()
            .email("login@email.com")
            .password("password123")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Test /login endpoint with invalid password")
    @Requirement("VCT-81")
    void testLoginWithInvalidPassword() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
            .name("Test User")
            .email("testlogin@email.com")
            .password("correctpassword")
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

        mockMvc.perform(post("/api/v1/auth/register")
            .param("role", "NORMAL_USER")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        AuthenticationRequest loginRequest = AuthenticationRequest.builder()
            .email("testlogin@email.com")
            .password("wrongpassword")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test /login endpoint with non-existent user")
    @Requirement("VCT-81")
    void testLoginWithNonExistentUser() throws Exception {
        AuthenticationRequest loginRequest = AuthenticationRequest.builder()
            .email("nonexistent@email.com")
            .password("password123")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test /login endpoint with invalid JSON")
    @Requirement("VCT-81")
    void testLoginWithInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test /login endpoint with missing email")
    @Requirement("VCT-81")
    void testLoginWithMissingEmail() throws Exception {
        String requestBody = "{ \"password\": \"password123\" }";

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test /login endpoint with missing password")
    @Requirement("VCT-81")
    void testLoginWithMissingPassword() throws Exception {
        String requestBody = "{ \"email\": \"test@email.com\" }";

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test /login endpoint with empty credentials")
    @Requirement("VCT-81")
    void testLoginWithEmptyCredentials() throws Exception {
        AuthenticationRequest loginRequest = AuthenticationRequest.builder()
            .email("")
            .password("")
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isForbidden());
    }
}
