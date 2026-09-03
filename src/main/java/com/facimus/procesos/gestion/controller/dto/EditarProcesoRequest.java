package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.model.EstadoProceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditarProcesoRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "La descripcion es obligatoria.") String descripcion,
        @NotBlank(message = "La categoria es obligatoria.") String categoria,
        @NotNull(message = "Debe seleccionar un estado.") EstadoProceso estado) {
}
