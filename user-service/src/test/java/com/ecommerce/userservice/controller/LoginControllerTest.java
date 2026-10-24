package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.LoginRequestDTO;
import com.ecommerce.userservice.exception.LoginFailedException;
import com.ecommerce.userservice.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerTest {

    private LoginService loginService;
    private LoginController loginController;

    @BeforeEach
    void preparar() {
        loginService = mock(LoginService.class);
        loginController = new LoginController(loginService);
    }

    @Test
    void loginDevuelveElTokenEnElHeaderAuthorization() {
        LoginRequestDTO request = new LoginRequestDTO("isaac@example.com", "Clave123!");
        when(loginService.login(any(LoginRequestDTO.class))).thenReturn(new com.ecommerce.userservice.dto.AuthResponseDTO("jwt-token", java.time.Instant.now(), "refresh", java.time.Instant.now()));

        ResponseEntity<com.ecommerce.userservice.dto.AuthResponseDTO> resultado = loginController.login(request);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertEquals("jwt-token", resultado.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("refresh", resultado.getBody().refreshToken());
        verify(loginService).login(request);
    }

    @Test
    void loginPropagaElFalloDeCredenciales() {
        LoginRequestDTO request = new LoginRequestDTO("isaac@example.com", "Incorrecta1!");
        when(loginService.login(any(LoginRequestDTO.class))).thenThrow(new LoginFailedException());

        assertThrows(LoginFailedException.class, () -> loginController.login(request));
    }
}
