package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.validation.ValidEmail;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequestDTO(
        @Nullable
        @Pattern(
                regexp = "[\\p{L}]+(?:[ ]+[\\p{L}]+)*",
                message = "El nombre solo puede contener letras y espacios"
        )
        String name,

        @Nullable
        @Pattern(
                regexp = "[\\p{L}]+(?:[ ]+[\\p{L}]+)*",
                message = "El apellido paterno solo puede contener letras y espacios"
        )
        String paternalName,

        @Nullable
        @Pattern(
                regexp = "[\\p{L}]+(?:[ ]+[\\p{L}]+)*",
                message = "El apellido materno solo puede contener letras y espacios"
        )
        String maternalName,

        @Nullable
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z]{2,})+$",
                message = "El correo no tiene un formato válido"
        )
        @ValidEmail
        String email
){
}
