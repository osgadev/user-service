package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.config.TimeConfig;
import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.exception.*;
import com.ecommerce.userservice.security.config.SecurityConfig;
import com.ecommerce.userservice.security.config.jwt.*;
import com.ecommerce.userservice.service.LoginService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthEndpointsTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtFilter.class, TimeConfig.class,
            LoginController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean LoginService loginService() { return mock(LoginService.class); }
        @Bean UserDetailsService userDetailsService() { return mock(UserDetailsService.class); }
        @Bean JwtUtil jwtUtil() { return new JwtUtil("test-secret-at-least-32-bytes-long"); }
    }
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    LoginService service;
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        service = context.getBean(LoginService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void close() { context.close(); }

    @Test void consultaPublicaYRecursosProtegidos() throws Exception {
        when(service.getAuthenticationStatus(1L)).thenReturn(new AuthenticationStatusDTO(true));
        mvc.perform(get("/api/auth/users/1/authenticated")).andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/users/1")).andExpect(status().isUnauthorized());
    }

    @Test void refreshNoRequiereJwtYErroresUsanHandler() throws Exception {
        String refresh = "a".repeat(43);
        var response = new AuthResponseDTO("access", Instant.now(), "new", Instant.now());
        when(service.refresh(refresh)).thenReturn(response);
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer expired")
                .contentType("application/json").content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "access"))
                .andExpect(jsonPath("$.refreshToken").value("new"));

        when(service.refresh(refresh)).thenThrow(new InvalidRefreshTokenException());
        mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token inválido o vencido"));
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
