package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.service.UsersService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    // =========================================================
    // Operaciones CRUD
    // =========================================================

    //    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR', 'CLIENTE', 'USUARIO', 'VENDEDOR', 'REPARTIDOR')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {

        UserResponseDTO userResponse = usersService.findById(id);

        return ResponseEntity.ok(userResponse);
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(usersService.findAll());
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO userRequest) {

        UserResponseDTO usuarioGuardado = usersService.createUser(userRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioGuardado);
    }

    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR', 'CLIENTE', 'USUARIO', 'VENDEDOR', 'REPARTIDOR')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDTO userRequest) {

        UserResponseDTO usuarioActualizado = usersService.updateUser(id, userRequest);

        return ResponseEntity.ok(usuarioActualizado);
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserStatusDTO> activateUser(@PathVariable Long id) {

        UserStatusDTO userResponse = usersService.activateAndDeactivateUser(id, true);

        return ResponseEntity.ok(userResponse);
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserStatusDTO> deactivateUser(@PathVariable Long id) {

        UserStatusDTO userResponse = usersService.activateAndDeactivateUser(id, false);

        return ResponseEntity.ok(userResponse);
    }

    // =========================================================
    // Consultar por estado isActive
    // =========================================================

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @GetMapping("/{id}/isActive")
    public ResponseEntity<UserStatusDTO> getUserIsActive(@PathVariable Long id) {

        UserStatusDTO userResponse = usersService.findUserStatusById(id);

        return ResponseEntity.ok(userResponse);
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @GetMapping("/active")
    public ResponseEntity<List<UserResponseDTO>> getAllUsersActive() {

        List<UserResponseDTO> usersResponse = usersService.findAllUsersByIsActive(true);

        return ResponseEntity.ok(usersResponse);
    }


    // =========================================================
    // Consultar por Rol
    // =========================================================

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @PostMapping("/role")
    public ResponseEntity<List<UserResponseDTO>> getAllUsersByRole(
            @Valid @RequestBody UserRoleFilterDTO filter) {

        List<UserResponseDTO> usersResponse = usersService.findAllUsersByRole(filter.role());

        return ResponseEntity.ok(usersResponse);
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(@PathVariable Long id, @Valid @RequestBody UserRoleRequestDTO request) {
        UserResponseDTO response = usersService.updateUserRole(id, request.role());
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Cambiar Contraseña
    // =========================================================

    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR', 'CLIENTE', 'USUARIO', 'VENDEDOR', 'REPARTIDOR')")
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordRequestDTO passwordRequestDTO,
                                               //Contiene la información del usuario que hizo la petición
                                               //Se general al validarse que el JWT es valido
                                               //En el Controller se inyectan los valores en automatico si se define así
                                               Authentication authentication) {

        usersService.updatePassword(passwordRequestDTO, authentication.getName());

        return ResponseEntity.ok().build();
    }

    // =========================================================
    // Actualizar la informacion del usuario de la sesion actual
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequestDTO userRequest
    ) {

        UserResponseDTO usuarioActualizado = usersService.updateCurrentUser(authentication.getName(), userRequest);

        return ResponseEntity.ok(usuarioActualizado);
    }
    // =========================================================
    // Consultar Perfil del Usuario Autenticado (/api/users/me)
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        UserResponseDTO response = usersService.findByEmail(email);
        return ResponseEntity.ok(response);
    }
}