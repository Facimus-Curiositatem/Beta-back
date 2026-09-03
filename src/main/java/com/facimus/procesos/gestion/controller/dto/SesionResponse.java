package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.model.RolAcceso;

public record SesionResponse(Long empresaId, Long usuarioId, String nombre, String nombreEmpresa,
        RolAcceso rolAcceso) {
}
