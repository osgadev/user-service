package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.LoginRequestDTO;
import com.ecommerce.userservice.dto.AuthResponseDTO;
import com.ecommerce.userservice.dto.AuthenticationStatusDTO;
import com.ecommerce.userservice.exception.InvalidRefreshTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import com.ecommerce.userservice.exception.LoginFailedException;
import com.ecommerce.userservice.model.Users;
import com.ecommerce.userservice.repository.UsersRepository;
import com.ecommerce.userservice.security.config.jwt.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;
    private final Clock clock;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    public LoginService(AuthenticationManager authenticationManager,
                        JwtUtil jwtUtil, UsersRepository usersRepository, Clock clock){
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usersRepository = usersRepository;
        this.clock = clock;
    }

    // =========================================================
    // Login
    // =========================================================

    @Transactional(noRollbackFor = LoginFailedException.class)
    public AuthResponseDTO login(LoginRequestDTO login) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Users> optionalUser = usersRepository.findByEmailForUpdate(login.email());

        //Valida si la búsqueda por email devolvió un user
        if(optionalUser.isPresent()){
            Users user = optionalUser.get();

            if(isUserCurrentlyLocked(user, now)) throw new LoginFailedException();

            //Aplica para usuarios con bloqueo expirado
            if(hasExpiredLock(user, now)) resetBlockedState(user);
        }
        //Creamos un token usando las credenciales que ingresó el usuario en el login
        UsernamePasswordAuthenticationToken localToken =
                new UsernamePasswordAuthenticationToken(login.email(), login.password());

		/*
		Si se obtiene un valor es porque se logró la autenticación
		Si se logró, tenemos acceso métodos:
			authentication.isAuthenticated()
			authentication.getPrincipal()
				-> devuelve User[ username, password, enabled, accountNonExpired,
												  CredentialNonExpired, AccountNonLocked,
												  GrantedAuthorities[ROLE_ADMIN, random_access] ]
		*/
        try {
            Authentication authentication = this.authenticationManager.authenticate(localToken);

            //Ingresar mal el email o contraseña incrementa los intentos fallidos de login
        }catch (BadCredentialsException e){
            optionalUser.ifPresent(user -> registerFailedAttempt(user, now));
            throw new LoginFailedException();

            //Login fallido por cuenta inactiva, bloqueada, etc. no incrementa los intentos fallidos de login
        }catch (AuthenticationException e){
            throw new LoginFailedException();
        }

        Users user = optionalUser.orElseThrow(LoginFailedException::new);
        resetBlockedState(user);
        return createTokens(user);
    }

    // =========================================================
    // Bloqueo temporal por exceder intentos de Login y Desbloqueo
    // =========================================================

    //Verifica si el usuario está bloqueado temporalmente
    private Boolean isUserCurrentlyLocked(Users user, LocalDateTime now){
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }

    //Verifica si el usuario sí estaba bloqueado temporalmente y si ya pasó su bloqueo
    private  Boolean hasExpiredLock(Users user, LocalDateTime now){
        return user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now);
    }

    //Resetear a 0 los intentos de login fallidos y fecha de expiración de bloqueo a null
    //Desbloquea al usuario
    private void resetBlockedState(Users user){
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
    }

    //Incrementa los intentos de login fallido y verifica si debe BLOQUEAR temporalmente al usuario
    private void registerFailedAttempt(Users user, LocalDateTime now){
        int currentAttempts = user.getFailedLoginAttempts();
        int updateAttempts = Math.min(currentAttempts + 1, MAX_FAILED_ATTEMPTS);

        user.setFailedLoginAttempts(updateAttempts);

        if(user.getFailedLoginAttempts() == MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(
                    now.plusMinutes(LOCK_DURATION_MINUTES));
        }

        usersRepository.save(user);
    }

    // =========================================================
    // Creación de JWT y RefreshToken
    // =========================================================

    private AuthResponseDTO createTokens(Users user) {
        //Creación de JWT
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(30, ChronoUnit.MINUTES);
        Instant refreshExpiresAt = now.plus(7, ChronoUnit.DAYS);
        String jwtId = UUID.randomUUID().toString();
        String jwt = jwtUtil.create(user.getEmail(), jwtId, expiresAt);

        //Creación de RefreshToken
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setJwtId(jwtId);
        user.setJwtExpiresAt(expiresAt);
        user.setRefreshTokenHash(hashRefreshToken(refreshToken));
        user.setRefreshTokenExpiresAt(refreshExpiresAt);

        usersRepository.save(user);

        //Regresa id
        return new AuthResponseDTO(jwt, expiresAt, refreshToken, refreshExpiresAt);
    }

    //Refresca el Refresh Token (Recibe el Refresh Token No hasheado)
    @Transactional
    public AuthResponseDTO refresh(String refreshToken) {
        String refreshTokenHash = hashRefreshToken(refreshToken);
        Users user = usersRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!refreshTokenHash.equals(user.getRefreshTokenHash()) ||
                !Boolean.TRUE.equals(user.getIsActive()) ||
                user.getRefreshTokenExpiresAt() == null ||
                !user.getRefreshTokenExpiresAt().isAfter(clock.instant())) {
            throw new InvalidRefreshTokenException();
        }
        return createTokens(user);
    }

    //Hashea el Refresh Token
    private String hashRefreshToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    // =========================================================
    // Verificar si un usuario está AUTENTICADO
    // =========================================================

    @Transactional(readOnly = true)
    public AuthenticationStatusDTO getAuthenticationStatus(Long id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id debe ser mayor que cero");
        }
        boolean authenticated = usersRepository.findById(id).map(user ->
                Boolean.TRUE.equals(user.getIsActive()) && user.getJwtId() != null
                        && user.getJwtExpiresAt() != null
                        && user.getJwtExpiresAt().isAfter(clock.instant())).orElse(false);
        return new AuthenticationStatusDTO(authenticated);
    }
}
