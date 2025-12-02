package com.vaicomtudo.backend.boundary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
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
    @AfterEach
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
}
