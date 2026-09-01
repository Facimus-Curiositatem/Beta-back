package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.model.RolAcceso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioForm {

    @NotBlank(message = "El nombre es obligatorio.")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no es valido.")
    private String email;

    /** Solo obligatoria al crear; en edicion de rol se ignora. */
    private String password;

    @NotNull(message = "Debe seleccionar un rol de acceso.")
    private RolAcceso rolAcceso;
}
