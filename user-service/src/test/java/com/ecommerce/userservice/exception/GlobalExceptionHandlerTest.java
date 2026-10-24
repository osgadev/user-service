package com.ecommerce.userservice.exception;

import com.ecommerce.userservice.dto.UserRequestDTO;
import com.ecommerce.userservice.exception.dto.ErrorResponse;
import com.ecommerce.userservice.exception.dto.ErrorResponseValidation;
import com.ecommerce.userservice.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void preparar() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/users/1");
    }

    @Test
    void handleEmailAlreadyExists_devuelve409() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleEmailAlreadyExists(
                new EmailAlreadyExistsException("El correo ya se encuentra registrado"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(409, respuesta.getBody().status());
        assertEquals("Conflict", respuesta.getBody().error());
        assertEquals("El correo ya se encuentra registrado", respuesta.getBody().message());
        assertEquals("/api/users/1", respuesta.getBody().path());
    }

    @Test
    void handleUsersNotFound_devuelve404() {
        ResponseEntity<ErrorResponseValidation> respuesta = handler.handleUsersNotFound(
                new UsersNotFoundException("Usuario no encontrado"),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(404, respuesta.getBody().status());
        assertEquals("Not Found", respuesta.getBody().error());
        assertEquals("Usuario no encontrado", respuesta.getBody().validationErrors().get("usuario"));
        assertEquals("/api/users/1", respuesta.getBody().path());
    }

    @Test
    void handleRoleNotFound_devuelve404() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleRoleNotFound(
                new RoleNotFoundException("El rol no es válido"),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(404, respuesta.getBody().status());
        assertEquals("El rol no es válido", respuesta.getBody().message());
    }

    @Test
    void handleInvalidRequest_devuelve400() {
        HttpMessageNotReadableException excepcion = new HttpMessageNotReadableException(
                "JSON inválido",
                new MockHttpInputMessage("{}".getBytes(StandardCharsets.UTF_8))
        );

        ResponseEntity<Map<String, String>> respuesta = handler.handleInvalidRequest(excepcion);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals("Bad Request", respuesta.getBody().get("error"));
        assertEquals("El rol enviado no es válido.", respuesta.getBody().get("message"));
    }

    @Test
    void handleValidation_devuelve400ConLosErroresPorCampo() throws Exception {
        UserRequestDTO dto = new UserRequestDTO(
                "Isaac",
                "Lopez",
                "Perez",
                "isaac@dominio-invalido.com",
                "Clave123!"
        );
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(dto, "userRequest");
        bindingResult.rejectValue(
                "email",
                "ValidEmail",
                "El dominio del correo no está permitido"
        );

        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod(
                        "metodoDePrueba",
                        UserRequestDTO.class
                ),
                0
        );
        MethodArgumentNotValidException excepcion =
                new MethodArgumentNotValidException(parametro, bindingResult);

        ResponseEntity<ErrorResponseValidation> respuesta =
                handler.handleValidation(excepcion, request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(400, respuesta.getBody().status());
        assertEquals("Bad Request", respuesta.getBody().error());
        assertEquals(
                "El dominio del correo no está permitido",
                respuesta.getBody().validationErrors().get("email")
        );
    }

    @Test
    void handlePasswordException_devuelve400() {
        ResponseEntity<ErrorResponse> respuesta = handler.handlePasswordException(
                new PasswordException("currentPassword es Incorrecta"),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(400, respuesta.getBody().status());
        assertEquals("currentPassword es Incorrecta", respuesta.getBody().message());
    }

    @Test
    void handleLoginFailed_devuelve401() {
        ResponseEntity<ErrorResponse> respuesta = handler.LoginFailedException(
                new LoginFailedException(),
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(401, respuesta.getBody().status());
        assertEquals("Unauthorized", respuesta.getBody().error());
        assertEquals("Credenciales inválidas", respuesta.getBody().message());
    }

    @Test
    void handleResponseStatus_devuelveElEstadoDeLaExcepcion() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "size fuera de rango"),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(400, respuesta.getBody().status());
        assertEquals("Bad Request", respuesta.getBody().error());
        assertEquals("size fuera de rango", respuesta.getBody().message());
    }

    @Test
    void handleTypeMismatch_enumIncluyeLosValoresPermitidos() {
        MethodArgumentTypeMismatchException excepcion =
                new MethodArgumentTypeMismatchException(
                        "INVALIDO",
                        Role.class,
                        "role",
                        null,
                        null
                );

        ResponseEntity<ErrorResponse> respuesta =
                handler.handleTypeMismatch(excepcion, request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().message().contains("Valores permitidos"));
        assertTrue(respuesta.getBody().message().contains("ADMINISTRADOR"));
    }

    @Test
    void handleTypeMismatch_noEnumNoIncluyeValoresPermitidos() {
        MethodArgumentTypeMismatchException excepcion =
                new MethodArgumentTypeMismatchException(
                        "abc",
                        Long.class,
                        "id",
                        null,
                        null
                );

        ResponseEntity<ErrorResponse> respuesta =
                handler.handleTypeMismatch(excepcion, request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertFalse(respuesta.getBody().message().contains("Valores permitidos"));
    }

    @Test
    void handleTypeMismatch_tipoNuloNoIncluyeValoresPermitidos() {
        MethodArgumentTypeMismatchException excepcion =
                new MethodArgumentTypeMismatchException(
                        "abc",
                        null,
                        "id",
                        null,
                        null
                );

        ResponseEntity<ErrorResponse> respuesta =
                handler.handleTypeMismatch(excepcion, request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertFalse(respuesta.getBody().message().contains("Valores permitidos"));
    }

    @Test
    void handleAccessDenied_devuelve403() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleAccessDenied(
                new AccessDeniedException("denegado"),
                request
        );

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(403, respuesta.getBody().status());
        assertEquals("Forbidden", respuesta.getBody().error());
        assertEquals(
                "No tienes permisos para acceder a este recurso",
                respuesta.getBody().message()
        );
    }

    @SuppressWarnings("unused")
    private void metodoDePrueba(UserRequestDTO dto) {
    }
}
