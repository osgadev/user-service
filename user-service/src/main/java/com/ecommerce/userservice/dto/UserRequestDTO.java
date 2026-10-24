package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.validation.ValidEmail;
import com.ecommerce.userservice.validation.ValidName;
import com.ecommerce.userservice.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record UserRequestDTO (
    @NotBlank(message = "El nombre es obligatorio")
    @ValidName(message = "El nombre solo puede contener letras y espacios")
    String name,

    @NotBlank(message = "El apellido paterno es obligatorio")
    @ValidName(message = "El apellido paterno solo puede contener letras y espacios")
    String paternalName,

    @NotBlank(message = "El apellido materno es obligatorio")
    @ValidName(message = "El apellido materno solo puede contener letras y espacios")
    String maternalName,

    @NotBlank(message = "El email es obligatorio")
    @ValidEmail
    String email,

    @NotBlank(message = "El password es obligatorio")
    @ValidPassword
    String password
){
}
