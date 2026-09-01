package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.model.EstadoProceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcesoForm {

    @NotBlank(message = "El nombre es obligatorio.")
    private String nombre;

    @NotBlank(message = "La descripcion es obligatoria.")
    private String descripcion;

    @NotBlank(message = "La categoria es obligatoria.")
    private String categoria;

    @NotNull(message = "Debe seleccionar un estado.")
    private EstadoProceso estado = EstadoProceso.BORRADOR;
}
