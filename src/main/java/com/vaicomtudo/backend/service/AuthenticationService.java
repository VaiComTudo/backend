package com.vaicomtudo.backend.service;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vaicomtudo.backend.auth.AuthenticationRequest;
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

        // If the user already exists
        if (userRepository.findByAccountEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered.");
        }

        // If the user is under 18
        int age = Period.between(request.getBirthdate(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("User must be at least 18 years old.");
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

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByAccountEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
            .token(jwtToken)
            .build();
    }
}
