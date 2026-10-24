package com.ecommerce.userservice.security.config.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ecommerce.userservice.model.Users;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Clock;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final Clock clock;

    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, Clock clock) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) authenticate(header.substring(7));

        chain.doFilter(request, response);
    }

    // =========================================================
    // Crear una credencial y guardarla en SecurityContextHolder
    // =========================================================

    /*
     * En este punto la firma del JWT es válida y el JWT no ha expirado
     * Falta validar que el jwtId y jwtExpiresAt del JWT sean idénticos a los que están guardados en User
     * Esto asegura que aunque el usuario genere muchos JWT solo sea válido el último que generó
     */
    private void authenticate(String jwt) {
        try {
            var jwtDecoded = jwtUtil.verify(jwt);
            var details = userDetailsService.loadUserByUsername(jwtDecoded.getSubject());

            if (details instanceof Users user
                    && user.getIsActive() == true
                    && jwtDecoded.getId().equals(user.getJwtId())
                    && user.getJwtExpiresAt() != null
                    && user.getJwtExpiresAt().isAfter(clock.instant())
                    && user.getJwtExpiresAt().equals(jwtDecoded.getExpiresAtAsInstant())
            ) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        user.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JWTVerificationException | UsernameNotFoundException ex) {
            // Los recursos protegidos responden 401; refresh valida su propia credencial.
        }
    }
}
