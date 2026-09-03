package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.model.RolAcceso;

import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(
        @NotNull(message = "Debe seleccionar un rol de acceso.") RolAcceso rolAcceso) {
}
