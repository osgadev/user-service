package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.PasswordRequestDTO;
import com.ecommerce.userservice.dto.UpdateUserRequestDTO;
import com.ecommerce.userservice.dto.UserRequestDTO;
import com.ecommerce.userservice.dto.UserResponseDTO;
import com.ecommerce.userservice.dto.UserRoleFilterDTO;
import com.ecommerce.userservice.dto.UserRoleRequestDTO;
import com.ecommerce.userservice.dto.UserStatusDTO;
import com.ecommerce.userservice.exception.UsersNotFoundException;
import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersControllerTest {

    private static final String EMAIL = "isaac@gmail.com";

    private UsersService usersService;
    private UsersController usersController;
    private Authentication authentication;

    @BeforeEach
    void preparar() {
        usersService = mock(UsersService.class);
        usersController = new UsersController(usersService);
        authentication = mock(Authentication.class);
    }

    @Test
    void getUserByIdDevuelveElUsuario() {
        UserResponseDTO esperado = userResponse();
        when(usersService.findById(1L)).thenReturn(esperado);

        ResponseEntity<UserResponseDTO> resultado = usersController.getUserById(1L);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(usersService).findById(1L);
    }

    @Test
    void getAllUsersDevuelveLaLista() {
        List<UserResponseDTO> esperados = List.of(userResponse());
        when(usersService.findAll()).thenReturn(esperados);

        ResponseEntity<List<UserResponseDTO>> resultado = usersController.getAllUsers();

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperados, resultado.getBody());
        verify(usersService).findAll();
    }

    @Test
    void createUserDevuelve201ConElUsuarioCreado() {
        UserRequestDTO request = new UserRequestDTO(
                "Isaac", "Lopez", "Perez", EMAIL, "Clave123!");
        UserResponseDTO esperado = userResponse();
        when(usersService.createUser(request)).thenReturn(esperado);

        ResponseEntity<UserResponseDTO> resultado = usersController.createUser(request);

        assertEquals(HttpStatus.CREATED, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(usersService).createUser(request);
    }

    @Test
    void updateUserDevuelveElUsuarioActualizado() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos", "Garcia", "Sanchez", "nuevo@gmail.com");
        UserResponseDTO esperado = userResponse();
        when(usersService.updateUser(1L, request)).thenReturn(esperado);

        ResponseEntity<UserResponseDTO> resultado = usersController.updateUser(1L, request);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(usersService).updateUser(1L, request);
    }

    @Test
    void activateUserActivaElUsuario() {
        UserStatusDTO esperado = new UserStatusDTO(1L, true);
        when(usersService.activateAndDeactivateUser(1L, true)).thenReturn(esperado);

        ResponseEntity<UserStatusDTO> resultado = usersController.activateUser(1L);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(usersService).activateAndDeactivateUser(1L, true);
    }

    @Test
    void deactivateUserDesactivaElUsuario() {
        UserStatusDTO esperado = new UserStatusDTO(1L, false);
        when(usersService.activateAndDeactivateUser(1L, false)).thenReturn(esperado);

        ResponseEntity<UserStatusDTO> resultado = usersController.deactivateUser(1L);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(usersService).activateAndDeactivateUser(1L, false);
    }

    @Test
    void getUserIsActiveDevuelveElEstado() {
        UserStatusDTO esperado = new UserStatusDTO(1L, true);
        when(usersService.findUserStatusById(1L)).thenReturn(esperado);

        ResponseEntity<UserStatusDTO> resultado = usersController.getUserIsActive(1L);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(usersService).findUserStatusById(1L);
    }

    @Test
    void getAllUsersActiveDevuelveLosUsuariosActivos() {
        List<UserResponseDTO> esperados = List.of(userResponse());
        when(usersService.findAllUsersByIsActive(true)).thenReturn(esperados);

        ResponseEntity<List<UserResponseDTO>> resultado = usersController.getAllUsersActive();

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperados, resultado.getBody());
        verify(usersService).findAllUsersByIsActive(true);
    }

    @Test
    void getAllUsersByRoleDevuelveLosUsuariosDelRol() {
        UserRoleFilterDTO filtro = new UserRoleFilterDTO("ADMINISTRADOR");
        List<UserResponseDTO> esperados = List.of(userResponse());
        when(usersService.findAllUsersByRole("ADMINISTRADOR")).thenReturn(esperados);

        ResponseEntity<List<UserResponseDTO>> resultado = usersController.getAllUsersByRole(filtro);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperados, resultado.getBody());
        verify(usersService).findAllUsersByRole("ADMINISTRADOR");
    }

    @Test
    void updateUserRoleDevuelveElUsuarioActualizado() {
        UserRoleRequestDTO request = new UserRoleRequestDTO(Role.ADMINISTRADOR);
        UserResponseDTO esperado = userResponse();
        when(usersService.updateUserRole(1L, Role.ADMINISTRADOR)).thenReturn(esperado);

        ResponseEntity<UserResponseDTO> resultado = usersController.updateUserRole(1L, request);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(usersService).updateUserRole(1L, Role.ADMINISTRADOR);
    }

    @Test
    void updatePasswordUsaElEmailDeLaAutenticacion() {
        PasswordRequestDTO request = new PasswordRequestDTO(
                "Actual123!", "Nueva456@", "Nueva456@");
        when(authentication.getName()).thenReturn(EMAIL);

        ResponseEntity<Void> resultado = usersController.updatePassword(request, authentication);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        verify(usersService).updatePassword(any(PasswordRequestDTO.class), any(String.class));
        verify(usersService).updatePassword(request, EMAIL);
    }

    @Test
    void updateCurrentUserUsaElEmailDeLaAutenticacion() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos", "Garcia", "Sanchez", "nuevo@gmail.com");
        UserResponseDTO esperado = userResponse();
        when(authentication.getName()).thenReturn(EMAIL);
        when(usersService.updateCurrentUser(EMAIL, request)).thenReturn(esperado);

        ResponseEntity<UserResponseDTO> resultado = usersController.updateCurrentUser(authentication, request);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertSame(esperado, resultado.getBody());
        verify(authentication).getName();
        verify(usersService).updateCurrentUser(EMAIL, request);
    }

    @Test
    void updateCurrentUserPropagaLaExcepcionDelServicio() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos", null, null, null);
        when(authentication.getName()).thenReturn(EMAIL);
        when(usersService.updateCurrentUser(EMAIL, request))
                .thenThrow(new UsersNotFoundException("Usuario no encontrado"));

        assertThrows(
                UsersNotFoundException.class,
                () -> usersController.updateCurrentUser(authentication, request)
        );

        verify(usersService).updateCurrentUser(EMAIL, request);
    }

    private UserResponseDTO userResponse() {
        return new UserResponseDTO(
                1L, "CLI-001-C", "Isaac Lopez Perez", EMAIL, "CLIENTE", true,
                LocalDateTime.of(2026, 1, 1, 12, 0));
    }
}
