package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.validation.ValidEmail;
import com.ecommerce.userservice.validation.ValidName;
import jakarta.annotation.Nullable;

public record UpdateUserRequestDTO(

        @ValidName
        String name,

        @ValidName
        String paternalName,

        @ValidName
        String maternalName,

        @ValidEmail
        String email
){
}
