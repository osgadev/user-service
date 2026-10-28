package com.ecommerce.userservice.exception.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,         // Ej. 400, 404, 500
        String code,       // Ej. "Not Found", "Bad Request"
        String message,     // Ej. "El usuario no existe"
        String path        // Ej. "/api/users/15"
) {
}
