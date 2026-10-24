package com.ecommerce.userservice.security.config.jwt;

import com.ecommerce.userservice.model.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.FilterChain;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void aceptaSoloTokenActualYUsuarioActivo() throws Exception {
        var util = new JwtUtil("test-secret-at-least-32-bytes-long");
        var service = mock(UserDetailsService.class);
        var filter = new JwtFilter(util, service, Clock.systemUTC());
        var user = new Users();
        user.setEmail("a@example.com");
        user.setJwtId("current");
        user.setJwtExpiresAt(Instant.now().plusSeconds(1800).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        when(service.loadUserByUsername(user.getEmail())).thenReturn(user);
        String current = util.create(user.getEmail(), "current", user.getJwtExpiresAt());
        String old = util.create(user.getEmail(), "old", user.getJwtExpiresAt());
        for (String token : new String[]{current, old, "invalid"}) {
            SecurityContextHolder.clearContext();
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            var response = new MockHttpServletResponse();
            var chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            assertEquals(token.equals(current),
                    SecurityContextHolder.getContext().getAuthentication() != null);
            verify(chain).doFilter(request, response);
        }
        user.setIsActive(false);
        SecurityContextHolder.clearContext();
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + current);
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void permiteContinuarSinAuthorization() throws Exception {
        var filter = new JwtFilter(mock(JwtUtil.class), mock(UserDetailsService.class), Clock.systemUTC());
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
