package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.PasswordRequestDTO;
import com.ecommerce.userservice.dto.UpdateUserRequestDTO;
import com.ecommerce.userservice.dto.UserRequestDTO;
import com.ecommerce.userservice.dto.UserResponseDTO;
import com.ecommerce.userservice.dto.UserStatusDTO;
import com.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.ecommerce.userservice.exception.PasswordException;
import com.ecommerce.userservice.exception.RoleNotFoundException;
import com.ecommerce.userservice.exception.UsersNotFoundException;
import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.model.Users;
import com.ecommerce.userservice.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersServiceTest {

    private static final String EMAIL = "isaac@example.com";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 12, 0);

    private UsersRepository usersRepository;
    private UsersService usersService;
    private PasswordEncoder passwordEncoder;
    private Users usuario;

    @BeforeEach
    void preparar() {
        usersRepository = mock(UsersRepository.class);
        usersService = new UsersService(usersRepository);
        passwordEncoder = new BCryptPasswordEncoder();

        usuario = new Users();
        usuario.setId(1L);
        usuario.setUserIdentifier("CLI-001-C");
        usuario.setName("Isaac");
        usuario.setPaternalName("Lopez");
        usuario.setMaternalName("Perez");
        usuario.setEmail(EMAIL);
        usuario.setPassword("passwordDePrueba");
        usuario.setRole(Role.CLIENTE);
        usuario.setIsActive(true);
        usuario.setCreatedAt(CREATED_AT);
    }

    @Test
    void loadUserByUsername_devuelveElUsuario() {
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        UserDetails resultado = usersService.loadUserByUsername(EMAIL);

        assertSame(usuario, resultado);
        verify(usersRepository).findByEmail(EMAIL);
    }

    @Test
    void loadUserByUsername_lanzaExcepcionSiNoExiste() {
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> usersService.loadUserByUsername(EMAIL)
        );

        verify(usersRepository).findByEmail(EMAIL);
    }

    @Test
    void createUser_creaElUsuarioConRolUsuarioYIdentificador() {
        UserRequestDTO request = new UserRequestDTO(
                "Isaac",
                "Lopez",
                "Perez",
                EMAIL,
                "Clave123!"
        );

        when(usersRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usersRepository.save(any(Users.class))).thenAnswer(invocacion -> {
            Users guardado = invocacion.getArgument(0);
            if (guardado.getId() == null) {
                guardado.setId(1L);
            }
            guardado.setCreatedAt(CREATED_AT);
            return guardado;
        });

        UserResponseDTO respuesta = usersService.createUser(request);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.id());
        assertEquals("USR-001-I", respuesta.userIdentifier());
        assertEquals("Isaac Lopez Perez", respuesta.fullName());
        assertEquals(EMAIL, respuesta.email());
        assertEquals("USUARIO", respuesta.role());
        assertEquals(Boolean.TRUE, respuesta.isActive());
        assertEquals(CREATED_AT, respuesta.createdAt());

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).existsByEmail(EMAIL);
        verify(usersRepository, times(2)).save(captor.capture());

        Users guardado = captor.getAllValues().get(1);
        assertEquals("Isaac", guardado.getName());
        assertEquals("Lopez", guardado.getPaternalName());
        assertEquals("Perez", guardado.getMaternalName());
        assertEquals(EMAIL, guardado.getEmail());
        assertEquals(Role.USUARIO, guardado.getRole());
        assertEquals(Boolean.TRUE, guardado.getIsActive());
        assertTrue(passwordEncoder.matches(request.password(), guardado.getPassword()));
    }

    @Test
    void createUser_lanzaExcepcionSiElEmailYaExiste() {
        UserRequestDTO request = new UserRequestDTO(
                "Isaac",
                "Lopez",
                "Perez",
                EMAIL,
                "Clave123!"
        );

        when(usersRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> usersService.createUser(request)
        );

        verify(usersRepository).existsByEmail(EMAIL);
        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void findById_devuelveElUsuario() {
        when(usersRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UserResponseDTO respuesta = usersService.findById(1L);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.id());
        assertEquals("Isaac Lopez Perez", respuesta.fullName());
        assertEquals(EMAIL, respuesta.email());
        assertEquals("CLIENTE", respuesta.role());
        assertEquals(Boolean.TRUE, respuesta.isActive());
        assertEquals(CREATED_AT, respuesta.createdAt());

        verify(usersRepository).findById(1L);
    }

    @Test
    void findById_lanzaExcepcionSiNoExiste() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UsersNotFoundException.class, () -> usersService.findById(1L));

        verify(usersRepository).findById(1L);
    }

    @Test
    void findAll_devuelveTodosLosUsuarios() {
        when(usersRepository.findAll()).thenReturn(List.of(usuario));

        List<UserResponseDTO> respuesta = usersService.findAll();

        assertEquals(1, respuesta.size());
        assertEquals(1L, respuesta.get(0).id());
        assertEquals("Isaac Lopez Perez", respuesta.get(0).fullName());
        assertEquals(EMAIL, respuesta.get(0).email());
        assertEquals("CLIENTE", respuesta.get(0).role());

        verify(usersRepository).findAll();
    }

    @Test
    void findAll_devuelveListaVaciaSiNoHayUsuarios() {
        when(usersRepository.findAll()).thenReturn(List.of());

        List<UserResponseDTO> respuesta = usersService.findAll();

        assertTrue(respuesta.isEmpty());
        verify(usersRepository).findAll();
    }

    @Test
    void updateUser_actualizaTodosLosCampos() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos",
                "Garcia",
                "Sanchez",
                "nuevo@example.com"
        );

        when(usersRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usersRepository.existsByEmailAndIdNot("nuevo@example.com", 1L)).thenReturn(false);
        when(usersRepository.save(any(Users.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UserResponseDTO respuesta = usersService.updateUser(1L, request);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.id());
        assertEquals("Carlos Garcia Sanchez", respuesta.fullName());
        assertEquals("nuevo@example.com", respuesta.email());
        assertEquals("CLIENTE", respuesta.role());
        assertEquals(Boolean.TRUE, respuesta.isActive());
        assertEquals(CREATED_AT, respuesta.createdAt());

        assertEquals("Carlos", usuario.getName());
        assertEquals("Garcia", usuario.getPaternalName());
        assertEquals("Sanchez", usuario.getMaternalName());
        assertEquals("nuevo@example.com", usuario.getEmail());

        verify(usersRepository).findById(1L);
        verify(usersRepository).existsByEmailAndIdNot("nuevo@example.com", 1L);
        verify(usersRepository).save(usuario);
    }

    @Test
    void updateUser_mantieneLosCamposNulosOVacios() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(null, "", null, "");

        when(usersRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usersRepository.save(any(Users.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UserResponseDTO respuesta = usersService.updateUser(1L, request);

        assertEquals("Isaac Lopez Perez", respuesta.fullName());
        assertEquals(EMAIL, respuesta.email());
        assertEquals("Isaac", usuario.getName());
        assertEquals("Lopez", usuario.getPaternalName());
        assertEquals("Perez", usuario.getMaternalName());
        assertEquals(EMAIL, usuario.getEmail());

        verify(usersRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(usersRepository).save(usuario);
    }

    @Test
    void updateUser_lanzaExcepcionSiElEmailPerteneceAOtroUsuario() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                null,
                null,
                null,
                "nuevo@example.com"
        );

        when(usersRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usersRepository.existsByEmailAndIdNot("nuevo@example.com", 1L)).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> usersService.updateUser(1L, request)
        );

        verify(usersRepository).existsByEmailAndIdNot("nuevo@example.com", 1L);
        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updateUser_lanzaExcepcionSiNoExiste() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos",
                "Garcia",
                "Sanchez",
                "nuevo@example.com"
        );

        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UsersNotFoundException.class,
                () -> usersService.updateUser(1L, request)
        );

        verify(usersRepository).findById(1L);
        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void findByEmail_devuelveElUsuario() {
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        UserResponseDTO respuesta = usersService.findByEmail(EMAIL);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.id());
        assertEquals("Isaac Lopez Perez", respuesta.fullName());
        assertEquals(EMAIL, respuesta.email());
        assertEquals("CLIENTE", respuesta.role());
        assertEquals(Boolean.TRUE, respuesta.isActive());

        verify(usersRepository).findByEmail(EMAIL);
    }

    @Test
    void findByEmail_lanzaExcepcionSiNoExiste() {
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                UsersNotFoundException.class,
                () -> usersService.findByEmail(EMAIL)
        );

        verify(usersRepository).findByEmail(EMAIL);
    }

    @Test
    void activateAndDeactivateUser_actualizaElEstado() {
        when(usersRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usersRepository.save(any(Users.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UserStatusDTO respuesta = usersService.activateAndDeactivateUser(1L, false);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.id());
        assertFalse(respuesta.isActive());
        assertEquals(Boolean.FALSE, usuario.getIsActive());

        verify(usersRepository).findById(1L);
        verify(usersRepository).save(usuario);
    }

    @Test
    void activateAndDeactivateUser_lanzaExcepcionSiNoExiste() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UsersNotFoundException.class,
                () -> usersService.activateAndDeactivateUser(1L, true)
        );

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void findUserStatusById_devuelveElEstado() {
        when(usersRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UserStatusDTO respuesta = usersService.findUserStatusById(1L);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.id());
        assertTrue(respuesta.isActive());

        verify(usersRepository).findById(1L);
        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void findUserStatusById_lanzaExcepcionSiNoExiste() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UsersNotFoundException.class,
                () -> usersService.findUserStatusById(1L)
        );

        verify(usersRepository).findById(1L);
    }

    @Test
    void findAllUsersByIsActive_devuelveLosUsuariosFiltrados() {
        when(usersRepository.findAllByIsActive(true)).thenReturn(List.of(usuario));

        List<UserResponseDTO> respuesta = usersService.findAllUsersByIsActive(true);

        assertEquals(1, respuesta.size());
        assertEquals(1L, respuesta.get(0).id());
        assertEquals("Isaac Lopez Perez", respuesta.get(0).fullName());
        assertEquals("CLIENTE", respuesta.get(0).role());
        assertEquals(Boolean.TRUE, respuesta.get(0).isActive());

        verify(usersRepository).findAllByIsActive(true);
    }

    @Test
    void findAllUsersByRole_devuelveLosUsuariosDelRol() {
        when(usersRepository.findAllByRole(Role.CLIENTE)).thenReturn(List.of(usuario));

        List<UserResponseDTO> respuesta = usersService.findAllUsersByRole("cliente");

        assertEquals(1, respuesta.size());
        assertEquals(1L, respuesta.get(0).id());
        assertEquals("Isaac Lopez Perez", respuesta.get(0).fullName());
        assertEquals("CLIENTE", respuesta.get(0).role());

        verify(usersRepository).findAllByRole(Role.CLIENTE);
    }

    @Test
    void findAllUsersByRole_lanzaExcepcionSiElRolNoExiste() {
        assertThrows(
                RoleNotFoundException.class,
                () -> usersService.findAllUsersByRole("rol-invalido")
        );

        verify(usersRepository, never()).findAllByRole(any());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void updateUserRole_generaElIdentificadorSegunElRol(Role rol) {
        when(usersRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usersRepository.save(any(Users.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UserResponseDTO respuesta = usersService.updateUserRole(1L, rol);

        String identificadorEsperado = switch (rol) {
            case USUARIO -> "USR-001-I";
            case CLIENTE -> "CLI-001-C";
            case VENDEDOR -> "VEN-001-V";
            case REPARTIDOR -> "REP-001-R";
            case ADMINISTRADOR -> "ADM-001-A";
        };

        assertEquals(identificadorEsperado, respuesta.userIdentifier());
        assertEquals(rol.name(), respuesta.role());
        assertEquals(identificadorEsperado, usuario.getUserIdentifier());
        assertEquals(rol, usuario.getRole());

        verify(usersRepository).findById(1L);
        verify(usersRepository).save(usuario);
    }

    @Test
    void updateUserRole_lanzaExcepcionSiNoExiste() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UsersNotFoundException.class,
                () -> usersService.updateUserRole(1L, Role.ADMINISTRADOR)
        );

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updatePassword_actualizaLaContrasena() {
        usuario.setPassword(passwordEncoder.encode("Actual123!"));
        PasswordRequestDTO request = new PasswordRequestDTO(
                "Actual123!",
                "Nueva456@",
                "Nueva456@"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        usersService.updatePassword(request, EMAIL);

        assertTrue(passwordEncoder.matches("Nueva456@", usuario.getPassword()));
        verify(usersRepository).findByEmail(EMAIL);
        verify(usersRepository).save(usuario);
    }

    @Test
    void updatePassword_lanzaExcepcionSiElUsuarioNoExiste() {
        PasswordRequestDTO request = new PasswordRequestDTO(
                "Actual123!",
                "Nueva456@",
                "Nueva456@"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> usersService.updatePassword(request, EMAIL)
        );

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updatePassword_lanzaExcepcionSiLaContrasenaActualEsIncorrecta() {
        usuario.setPassword(passwordEncoder.encode("Actual123!"));
        PasswordRequestDTO request = new PasswordRequestDTO(
                "Incorrecta1!",
                "Nueva456@",
                "Nueva456@"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        assertThrows(
                PasswordException.class,
                () -> usersService.updatePassword(request, EMAIL)
        );

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updatePassword_lanzaExcepcionSiLaConfirmacionNoCoincide() {
        usuario.setPassword(passwordEncoder.encode("Actual123!"));
        PasswordRequestDTO request = new PasswordRequestDTO(
                "Actual123!",
                "Nueva456@",
                "Otra789#"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        assertThrows(
                PasswordException.class,
                () -> usersService.updatePassword(request, EMAIL)
        );

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updatePassword_lanzaExcepcionSiLaNuevaEsIgualALaActual() {
        usuario.setPassword(passwordEncoder.encode("Actual123!"));
        PasswordRequestDTO request = new PasswordRequestDTO(
                "Actual123!",
                "Actual123!",
                "Actual123!"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        assertThrows(
                PasswordException.class,
                () -> usersService.updatePassword(request, EMAIL)
        );

        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updateCurrentUser_actualizaTodosLosCamposEInvalidaTokens() {
        usuario.setJwtId("jwt-id");
        usuario.setJwtExpiresAt(Instant.parse("2026-01-01T13:00:00Z"));
        usuario.setRefreshTokenHash("refresh-hash");
        usuario.setRefreshTokenExpiresAt(Instant.parse("2026-01-02T12:00:00Z"));

        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos",
                "Garcia",
                "Sanchez",
                "nuevo@example.com"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usersRepository.existsByEmailAndIdNot("nuevo@example.com", 1L)).thenReturn(false);
        when(usersRepository.save(any(Users.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UserResponseDTO respuesta = usersService.updateCurrentUser(EMAIL, request);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.id());
        assertEquals("CLI-001-C", respuesta.userIdentifier());
        assertEquals("Carlos Garcia Sanchez", respuesta.fullName());
        assertEquals("nuevo@example.com", respuesta.email());
        assertEquals("CLIENTE", respuesta.role());
        assertEquals(Boolean.TRUE, respuesta.isActive());
        assertEquals(CREATED_AT, respuesta.createdAt());

        assertEquals("Carlos", usuario.getName());
        assertEquals("Garcia", usuario.getPaternalName());
        assertEquals("Sanchez", usuario.getMaternalName());
        assertEquals("nuevo@example.com", usuario.getEmail());
        assertNull(usuario.getJwtId());
        assertNull(usuario.getJwtExpiresAt());
        assertNull(usuario.getRefreshTokenHash());
        assertNull(usuario.getRefreshTokenExpiresAt());

        verify(usersRepository).findByEmail(EMAIL);
        verify(usersRepository).existsByEmailAndIdNot("nuevo@example.com", 1L);
        verify(usersRepository).save(usuario);
    }

    @Test
    void updateCurrentUser_mantieneLosCamposNulosOVacios() {
        usuario.setJwtId("jwt-id");
        usuario.setRefreshTokenHash("refresh-hash");

        UpdateUserRequestDTO request = new UpdateUserRequestDTO(null, "", null, "");

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usersRepository.save(any(Users.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UserResponseDTO respuesta = usersService.updateCurrentUser(EMAIL, request);

        assertEquals("Isaac Lopez Perez", respuesta.fullName());
        assertEquals(EMAIL, respuesta.email());
        assertEquals("Isaac", usuario.getName());
        assertEquals("Lopez", usuario.getPaternalName());
        assertEquals("Perez", usuario.getMaternalName());
        assertEquals(EMAIL, usuario.getEmail());
        assertEquals("jwt-id", usuario.getJwtId());
        assertEquals("refresh-hash", usuario.getRefreshTokenHash());

        verify(usersRepository).findByEmail(EMAIL);
        verify(usersRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(usersRepository).save(usuario);
    }

    @Test
    void updateCurrentUser_noInvalidaTokensSiElEmailNoCambia() {
        usuario.setJwtId("jwt-id");
        usuario.setJwtExpiresAt(Instant.parse("2026-01-01T13:00:00Z"));
        usuario.setRefreshTokenHash("refresh-hash");
        usuario.setRefreshTokenExpiresAt(Instant.parse("2026-01-02T12:00:00Z"));

        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos",
                null,
                null,
                EMAIL
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usersRepository.existsByEmailAndIdNot(EMAIL, 1L)).thenReturn(false);
        when(usersRepository.save(any(Users.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UserResponseDTO respuesta = usersService.updateCurrentUser(EMAIL, request);

        assertEquals("Carlos Lopez Perez", respuesta.fullName());
        assertEquals(EMAIL, respuesta.email());
        assertEquals("Carlos", usuario.getName());
        assertEquals(EMAIL, usuario.getEmail());
        assertEquals("jwt-id", usuario.getJwtId());
        assertEquals("refresh-hash", usuario.getRefreshTokenHash());

        verify(usersRepository).existsByEmailAndIdNot(EMAIL, 1L);
        verify(usersRepository).save(usuario);
    }

    @Test
    void updateCurrentUser_lanzaExcepcionSiElEmailPerteneceAOtroUsuario() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                null,
                null,
                null,
                "nuevo@example.com"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usersRepository.existsByEmailAndIdNot("nuevo@example.com", 1L)).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> usersService.updateCurrentUser(EMAIL, request)
        );

        assertEquals(EMAIL, usuario.getEmail());
        verify(usersRepository).findByEmail(EMAIL);
        verify(usersRepository).existsByEmailAndIdNot("nuevo@example.com", 1L);
        verify(usersRepository, never()).save(any(Users.class));
    }

    @Test
    void updateCurrentUser_lanzaExcepcionSiNoExiste() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "Carlos",
                "Garcia",
                "Sanchez",
                "nuevo@example.com"
        );

        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                UsersNotFoundException.class,
                () -> usersService.updateCurrentUser(EMAIL, request)
        );

        verify(usersRepository).findByEmail(EMAIL);
        verify(usersRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(usersRepository, never()).save(any(Users.class));
    }
}
