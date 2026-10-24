package com.ecommerce.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Getter @Setter
@NoArgsConstructor //lo pide jpa
@Entity
@Table(name = "users")
public class Users implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    @Column(name = "user_identifier", unique = true, length = 20) // se agrega el identificador compuesto
    private String userIdentifier;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "paternal_lastname", nullable = false, length = 100)
    private String paternalName;

    @Column(name = "maternal_lastname", nullable = false, length = 100)
    private String maternalName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 45)
    private Role role = Role.CLIENTE;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp //La fecha la agrega hibernate justo antes de guardar Users en la BD
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "jwt_id", length = 36)
    private String jwtId;

    @Column(name = "jwt_expires_at")
    private Instant jwtExpiresAt;

    @Column(name = "refresh_token_hash", length = 64, unique = true)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    //constructor para nuevos usuarios
    public Users(String name, String paternalName, String maternalName, String email, String password, Role role, Boolean isActive) {
        this.name = name;
        this.paternalName = paternalName;
        this.maternalName = maternalName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //Devuelve una lista con nuestros roles
        return List.of(new SimpleGrantedAuthority(this.role.name()));
    }

    @Override
    public String getUsername() {
        //Devuelve el username (en nuestro caso Email)
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        //Devuelve el estado de la cuenta activa/inactiva
        return this.isActive;
    }
}