package com.emia.archives.controller;

import com.emia.archives.dto.request.LoginRequest;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.dto.response.LoginResponse;
import com.emia.archives.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Connexion réussie"));
    }
}
