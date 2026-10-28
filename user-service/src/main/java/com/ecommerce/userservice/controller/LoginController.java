package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginService loginService;
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return tokenResponse(loginService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        AuthResponseDTO response = loginService.refresh(request.refreshToken());
        return tokenResponse(response); // Cambiado de ResponseEntity.ok(response) a tokenResponse(response)
    }

    @GetMapping("/users/{id}/authenticated")
    public ResponseEntity<AuthenticationStatusDTO> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(loginService.getAuthenticationStatus(id));
    }

    private ResponseEntity<AuthResponseDTO> tokenResponse(AuthResponseDTO response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.AUTHORIZATION, response.jwt()).body(response);
    }
}
