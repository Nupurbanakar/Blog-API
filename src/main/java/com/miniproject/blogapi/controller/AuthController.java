package com.miniproject.blogapi.controller;

import com.miniproject.blogapi.dto.LoginRequest;
import com.miniproject.blogapi.dto.RefreshRequest;
import com.miniproject.blogapi.dto.TokenResponse;
import com.miniproject.blogapi.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String accessToken = jwtService.generateAccessToken(request.getUsername());
        String refreshToken = jwtService.generateRefreshToken(request.getUsername());

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, "Bearer"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String token = request.getRefreshToken();

        if (!jwtService.isTokenValid(token) || !"refresh".equals(jwtService.extractTokenType(token))) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsername(token);
        String newAccessToken = jwtService.generateAccessToken(username);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, token, "Bearer"));
    }
}
