package com.facimus.procesos.modelado.controller.dto;

import java.util.Set;

import com.facimus.procesos.gestion.model.RolAcceso;

import jakarta.validation.constraints.NotEmpty;

public record ConfigurarPermisosPoolRequest(
        @NotEmpty(message = "Debe indicar al menos un rol con permiso de edicion.") Set<RolAcceso> rolesEdicion) {
}
