package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.LoginRequestDTO;
import com.ecommerce.userservice.exception.LoginFailedException;
import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.model.Users;
import com.ecommerce.userservice.repository.UsersRepository;
import com.ecommerce.userservice.security.config.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private static final String EMAIL = "isaac@example.com";
    private static final String PASSWORD = "Clave123!";
    private static final String TOKEN = "jwt-token";
    private static final Instant INSTANTE_ACTUAL = Instant.parse("2026-01-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(INSTANTE_ACTUAL, ZoneOffset.UTC);
    private static final LocalDateTime AHORA =
            LocalDateTime.ofInstant(INSTANTE_ACTUAL, ZoneOffset.UTC);

    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private UsersRepository usersRepository;
    private LoginService loginService;
    private Users usuario;
    private LoginRequestDTO request;

    @BeforeEach
    void preparar() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtUtil = mock(JwtUtil.class);
        usersRepository = mock(UsersRepository.class);
        loginService = new LoginService(
                authenticationManager,
                jwtUtil,
                usersRepository,
                CLOCK
        );

        usuario = new Users();
        usuario.setId(1L);
        usuario.setEmail(EMAIL);
        usuario.setPassword("password-codificada");
        usuario.setRole(Role.CLIENTE);
        usuario.setFailedLoginAttempts(0);
        usuario.setLockedUntil(null);

        request = new LoginRequestDTO(EMAIL, PASSWORD);
    }

    @Test
    void login_exitoso_usuarioExistente() {
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtUtil.create(eq(EMAIL), eq(1L), eq("CLIENTE"), any(String.class), any(Instant.class))).thenReturn(TOKEN);

        var resultado = loginService.login(request);

        assertEquals(TOKEN, resultado.jwt());
        assertEquals(0, usuario.getFailedLoginAttempts().intValue());
        assertNull(usuario.getLockedUntil());

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken enviado =
                (UsernamePasswordAuthenticationToken) captor.getValue();
        assertEquals(EMAIL, enviado.getPrincipal());
        assertEquals(PASSWORD, enviado.getCredentials());

        verify(usersRepository).save(usuario);
        verify(jwtUtil).create(eq(EMAIL), eq(1L), eq("CLIENTE"), any(String.class), eq(INSTANTE_ACTUAL.plusSeconds(1800)));
    }

    @Test
    void login_noEmiteTokensSiNoExisteElUsuario() {
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        assertThrows(LoginFailedException.class, () -> loginService.login(request));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_exitoso_reseteaIntentosPrevios() {
        usuario.setFailedLoginAttempts(2);
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtUtil.create(eq(EMAIL), eq(1L), eq("CLIENTE"), any(String.class), any(Instant.class))).thenReturn(TOKEN);

        var resultado = loginService.login(request);

        assertEquals(TOKEN, resultado.jwt());
        assertEquals(0, usuario.getFailedLoginAttempts().intValue());
        assertNull(usuario.getLockedUntil());
        verify(usersRepository).save(usuario);
    }

    @Test
    void login_usuarioBloqueado_lanzaExcepcion() {
        usuario.setLockedUntil(AHORA.plusMinutes(1));
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));

        LoginFailedException excepcion = assertThrows(
                LoginFailedException.class,
                () -> loginService.login(request)
        );

        assertEquals("Credenciales inválidas", excepcion.getMessage());
        verifyNoInteractions(authenticationManager);
        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void login_bloqueoExpirado_reseteaYLoginExitoso() {
        usuario.setFailedLoginAttempts(3);
        usuario.setLockedUntil(AHORA.minusMinutes(1));
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtUtil.create(eq(EMAIL), eq(1L), eq("CLIENTE"), any(String.class), any(Instant.class))).thenReturn(TOKEN);

        var resultado = loginService.login(request);

        assertEquals(TOKEN, resultado.jwt());
        assertEquals(0, usuario.getFailedLoginAttempts().intValue());
        assertNull(usuario.getLockedUntil());
        verify(usersRepository).save(usuario);
    }

    @Test
    void login_credencialesInvalidas_registraElPrimerIntento() {
        usuario.setFailedLoginAttempts(0);
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThrows(LoginFailedException.class, () -> loginService.login(request));

        assertEquals(1, usuario.getFailedLoginAttempts().intValue());
        assertNull(usuario.getLockedUntil());
        verify(usersRepository).save(usuario);
    }

    @Test
    void login_credencialesInvalidas_tercerIntentoBloqueaAlUsuario() {
        usuario.setFailedLoginAttempts(2);
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThrows(LoginFailedException.class, () -> loginService.login(request));

        assertEquals(3, usuario.getFailedLoginAttempts().intValue());
        assertEquals(AHORA.plusMinutes(5), usuario.getLockedUntil());
        verify(usersRepository).save(usuario);
    }

    @Test
    void login_credencialesInvalidas_mantieneElTopeDeIntentos() {
        usuario.setFailedLoginAttempts(5);
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThrows(LoginFailedException.class, () -> loginService.login(request));

        assertEquals(3, usuario.getFailedLoginAttempts().intValue());
        assertEquals(AHORA.plusMinutes(5), usuario.getLockedUntil());
        verify(usersRepository).save(usuario);
    }

    @Test
    void login_credencialesInvalidas_usuarioInexistente() {
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThrows(LoginFailedException.class, () -> loginService.login(request));

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void login_errorDeAutenticacion_noIncrementaLosIntentos() {
        usuario.setFailedLoginAttempts(0);
        when(usersRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Cuenta deshabilitada"));

        assertThrows(LoginFailedException.class, () -> loginService.login(request));

        assertEquals(0, usuario.getFailedLoginAttempts().intValue());
        assertNull(usuario.getLockedUntil());
        verify(usersRepository, never()).save(any(Users.class));
    }
}
