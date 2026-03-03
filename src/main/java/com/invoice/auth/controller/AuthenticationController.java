package com.invoice.auth.controller;

import com.invoice.auth.dto.RegisterUserDto;
import com.invoice.auth.dto.LoginResponse;
import com.invoice.auth.dto.LoginUserDto;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.AuthenticationService;
import com.invoice.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);

        LoginResponse loginResponse = LoginResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.extractClaim(jwtToken, claims -> claims.getExpiration().getTime()))
                .id(authenticatedUser.getId())
                .username(authenticatedUser.getUsername())
                .email(authenticatedUser.getEmail())
                .role(authenticatedUser.getRole().name())
                .contactNumber(authenticatedUser.getContactNumber())
                .build();

        return ResponseEntity.ok(loginResponse);
    }
}
