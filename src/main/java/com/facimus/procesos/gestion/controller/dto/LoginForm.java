package com.facimus.procesos.gestion.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "El correo es obligatorio.")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria.")
    private String password;
}
