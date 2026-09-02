package com.facimus.procesos.gestion.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolProcesoForm {

    @NotBlank(message = "El nombre del rol es obligatorio.")
    private String nombre;

    private String descripcion;
}
