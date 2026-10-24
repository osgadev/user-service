package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordRequestDTO(
        @NotBlank
        String currentPassword,

        @NotBlank(message = "El newPassword es obligatorio")
        @ValidPassword
        String newPassword,

        @NotBlank(message = "El currentPassword es obligatorio")
        @ValidPassword
        String confirmPassword
) {
}
