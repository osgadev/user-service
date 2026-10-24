package com.ecommerce.userservice.dto;

import java.time.Instant;

public record AuthResponseDTO(
        String jwt,
        Instant jwtExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
