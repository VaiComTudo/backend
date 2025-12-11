package com.vaicomtudo.backend.auth;

import java.time.LocalDate;

import com.vaicomtudo.backend.data.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
    private LocalDate birthdate;
}
