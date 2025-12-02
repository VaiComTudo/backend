package com.vaicomtudo.backend.boundary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.auth.AuthenticationRequest;
import com.vaicomtudo.backend.auth.AuthenticationResponse;
import com.vaicomtudo.backend.auth.RegisterRequest;
import com.vaicomtudo.backend.data.entity.Role;
import com.vaicomtudo.backend.service.AuthenticationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
        @RequestBody RegisterRequest request,
        @RequestParam Role role
    ) {
        log.info("Registration request for email={}", request.getEmail());
        return ResponseEntity.ok(authenticationService.register(request, role));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
        @RequestBody AuthenticationRequest request
    ) {
        log.info("Login request for email={}", request.getEmail());
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}
