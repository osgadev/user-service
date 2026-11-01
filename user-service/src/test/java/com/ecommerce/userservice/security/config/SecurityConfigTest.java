package com.ecommerce.userservice.security.config;

import com.ecommerce.userservice.security.config.jwt.JwtFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class SecurityConfigTest {

    private JwtFilter jwtFilter;
    private SecurityConfig securityConfig;

    @BeforeEach
    void preparar() {
        jwtFilter = mock(JwtFilter.class);
        securityConfig = new SecurityConfig(jwtFilter);
    }

    @Test
    void passwordEncoderDevuelveBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    void corsConfigurationSourcePermiteElFrontendYLosMetodosNecesarios() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");

        CorsConfiguration configuracion = source.getCorsConfiguration(request);

        assertNotNull(configuracion);
        assertEquals(List.of("http://localhost:5173"), configuracion.getAllowedOrigins());
        assertTrue(configuracion.getAllowedMethods().contains("PATCH"));
        assertTrue(configuracion.getAllowedMethods().contains("DELETE"));
        assertTrue(configuracion.getAllowedHeaders().contains("Authorization"));
        assertEquals(Boolean.TRUE, configuracion.getAllowCredentials());
    }

    @Test
    void authenticationManagerDelegaEnLaConfiguracion() throws Exception {
        AuthenticationConfiguration configuration = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(configuration.getAuthenticationManager()).thenReturn(manager);

        assertSame(manager, securityConfig.authenticationManager(configuration));
    }

    @Test
    void filterChainConfiguraLaSeguridadYAgregaElFiltroJwt() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(http.csrf(any())).thenReturn(http);
        when(http.cors(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.exceptionHandling(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.addFilterBefore(any(Filter.class), any())).thenReturn(http);
        when(http.build()).thenReturn(filterChain);

        assertSame(filterChain, securityConfig.filterChain(http));
        verify(http).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void accessDeniedHandlerDevuelveJsonConEstado403() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        when(http.csrf(any())).thenReturn(http);
        when(http.cors(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.addFilterBefore(any(Filter.class), any())).thenReturn(http);
        when(http.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        final AccessDeniedHandler[] handler = new AccessDeniedHandler[1];
        when(http.exceptionHandling(any())).thenAnswer(invocation -> {
            ExceptionHandlingConfigurer<HttpSecurity> configurer = mock(ExceptionHandlingConfigurer.class);
            when(configurer.authenticationEntryPoint(any())).thenReturn(configurer);
            Customizer<ExceptionHandlingConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            customizer.customize(configurer);
            ArgumentCaptor<AccessDeniedHandler> captor = ArgumentCaptor.forClass(AccessDeniedHandler.class);
            verify(configurer).accessDeniedHandler(captor.capture());
            handler[0] = captor.getValue();
            return http;
        });

        securityConfig.filterChain(http);
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler[0].handle(new MockHttpServletRequest(), response, new AccessDeniedException("Sin permiso"));

        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals("{\"status\":403,\"code\":\"Forbidden\",\"message\":\"No tienes permisos para acceder a este recurso\"}",
                response.getContentAsString());
    }
}
