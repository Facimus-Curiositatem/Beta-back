package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.model.PermisoEstructura;
import com.facimus.procesos.gestion.model.RolAcceso;

public record PermisoEstructuraResponse(
        RolAcceso rolAcceso,
        boolean crearPool,
        boolean editarPool,
        boolean eliminarPool,
        boolean crearLane,
        boolean editarLane,
        boolean eliminarLane) {

    public static PermisoEstructuraResponse of(PermisoEstructura permiso) {
        return new PermisoEstructuraResponse(permiso.getRolAcceso(), permiso.isCrearPool(),
                permiso.isEditarPool(), permiso.isEliminarPool(), permiso.isCrearLane(),
                permiso.isEditarLane(), permiso.isEliminarLane());
    }
}
