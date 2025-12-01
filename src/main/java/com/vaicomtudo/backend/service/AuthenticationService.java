package com.vaicomtudo.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vaicomtudo.backend.auth.AuthenticationResponse;
import com.vaicomtudo.backend.auth.RegisterRequest;
import com.vaicomtudo.backend.data.entity.Account;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request, Role role) {
        if (userRepository.findByAccountEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered.");
        }

        // Create account
        Account account = Account.builder()
            .name(request.getName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .build();

        // Create user
        User user = new User();
        user.setAccount(account);
        user.setBirthdate(request.getBirthdate());
        user.setRating(0.0);
        user.setRole(role);
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
            .token(jwtToken)
            .build();
    }
}
