package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.LoginRequestDTO;
import com.ecommerce.userservice.exception.InvalidRefreshTokenException;
import com.ecommerce.userservice.model.Users;
import com.ecommerce.userservice.repository.UsersRepository;
import com.ecommerce.userservice.security.config.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshSessionTest {
    final Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    final Users user = new Users();
    final UsersRepository repository = mock(UsersRepository.class);
    final JwtUtil jwt = new JwtUtil("test-secret-at-least-32-bytes-long");
    LoginService service;

    @BeforeEach void setup() {
        user.setId(1L);
        user.setEmail("a@example.com");
        service = new LoginService(mock(AuthenticationManager.class), jwt, repository,
                Clock.fixed(now, ZoneOffset.UTC));
        when(repository.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.findByRefreshTokenHash(anyString())).thenAnswer(call ->
                call.getArgument(0).equals(user.getRefreshTokenHash())
                        ? Optional.of(user) : Optional.empty());
    }

    @Test void loginGuardaHashYRenovacionReemplazaAmbasCredenciales() {
        var first = service.login(new LoginRequestDTO(user.getEmail(), "password"));
        String oldId = user.getJwtId();
        assertNotEquals(first.refreshToken(), user.getRefreshTokenHash());
        assertEquals(64, user.getRefreshTokenHash().length());
        assertEquals(43, first.refreshToken().length());
        assertEquals(now.plusSeconds(1800), first.jwtExpiresAt());
        assertTrue(service.getAuthenticationStatus(1L).authenticated());

        // Vencer el acceso no impide usar la credencial de renovación.
        user.setJwtExpiresAt(now);
        assertFalse(service.getAuthenticationStatus(1L).authenticated());
        var second = service.refresh(first.refreshToken());
        assertNotEquals(oldId, user.getJwtId());
        assertNotEquals(first.jwt(), second.jwt());
        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertEquals(user.getJwtId(), jwt.verify(second.jwt()).getId());
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh(first.refreshToken()));

        var third = service.login(new LoginRequestDTO(user.getEmail(), "password"));
        assertNotEquals(second.jwt(), third.jwt());
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh(second.refreshToken()));
    }

    @Test void rechazaRefreshVencidoInactivoNuloODesconocido() {
        var tokens = service.login(new LoginRequestDTO(user.getEmail(), "password"));
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh("desconocido"));
        user.setRefreshTokenExpiresAt(now);
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh(tokens.refreshToken()));
        user.setRefreshTokenExpiresAt(null);
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh(tokens.refreshToken()));
        user.setRefreshTokenExpiresAt(now.plusSeconds(60));
        user.setIsActive(false);
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh(tokens.refreshToken()));
    }

    @Test void estadoContemplaUsuariosSinSesionInactivosYNoExistentes() {
        assertFalse(service.getAuthenticationStatus(1L).authenticated());
        assertFalse(service.getAuthenticationStatus(2L).authenticated());
        assertThrows(ResponseStatusException.class, () -> service.getAuthenticationStatus(0L));
        user.setJwtId("id");
        assertFalse(service.getAuthenticationStatus(1L).authenticated());
        user.setJwtExpiresAt(now.plusSeconds(60));
        user.setIsActive(false);
        assertFalse(service.getAuthenticationStatus(1L).authenticated());
        user.setIsActive(true);
        assertTrue(service.getAuthenticationStatus(1L).authenticated());
        user.setRefreshTokenHash("hash-de-prueba");
        user.setRefreshTokenExpiresAt(now.plusSeconds(3600));
        new UsersService(repository).invalidateTokens(user);
        assertFalse(service.getAuthenticationStatus(1L).authenticated());
        assertNull(user.getJwtId());
        assertNull(user.getJwtExpiresAt());
        assertNull(user.getRefreshTokenHash());
        assertNull(user.getRefreshTokenExpiresAt());
    }
}
