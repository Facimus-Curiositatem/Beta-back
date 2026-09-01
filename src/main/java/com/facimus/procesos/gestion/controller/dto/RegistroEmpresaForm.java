package com.facimus.procesos.gestion.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroEmpresaForm {

    @NotBlank(message = "El nombre de la empresa es obligatorio.")
    private String nombreEmpresa;

    @NotBlank(message = "El NIT es obligatorio.")
    private String nit;

    @NotBlank(message = "El correo de contacto es obligatorio.")
    @Email(message = "El correo de contacto no es valido.")
    private String correoContacto;

    @NotBlank(message = "El nombre del administrador es obligatorio.")
    private String nombreAdmin;

    @NotBlank(message = "El correo del administrador es obligatorio.")
    @Email(message = "El correo del administrador no es valido.")
    private String emailAdmin;

    @NotBlank(message = "La contrasena es obligatoria.")
    @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres.")
    private String passwordAdmin;
}
