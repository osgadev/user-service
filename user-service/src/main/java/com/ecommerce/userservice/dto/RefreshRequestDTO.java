package com.ecommerce.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RefreshRequestDTO(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}")
        String refreshToken
) {
}
