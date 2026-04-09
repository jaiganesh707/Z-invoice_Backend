package com.invoice.auth.controller;

import com.invoice.auth.dto.*;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.AuthenticationService;
import com.invoice.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("#### AuthenticationController initialized at /auth ####");
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Auth System Online: " + java.time.LocalDateTime.now());
    }

    @PostMapping("/signup")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        return ResponseEntity.ok(authenticationService.signup(registerUserDto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);

        return ResponseEntity.ok(LoginResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.extractClaim(jwtToken, claims -> claims.getExpiration().getTime()))
                .id(authenticatedUser.getId())
                .username(authenticatedUser.getUsername())
                .email(authenticatedUser.getEmail())
                .role(authenticatedUser.getRole().name())
                .contactNumber(authenticatedUser.getContactNumber())
                .parentUserId(
                        authenticatedUser.getParentUser() != null ? authenticatedUser.getParentUser().getId() : null)
                .build());
    }
}
