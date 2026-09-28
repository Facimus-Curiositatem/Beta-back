package com.facimus.procesos.gestion.controller.dto;

import java.util.List;

import com.facimus.procesos.gestion.model.RolProceso;

public record RolProcesoConsultaResponse(Long id, String nombre, String descripcion,
        List<String> procesos, boolean enUso) {

    public static RolProcesoConsultaResponse of(RolProceso rol, List<String> procesos) {
        return new RolProcesoConsultaResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(),
                procesos, !procesos.isEmpty());
    }
}
