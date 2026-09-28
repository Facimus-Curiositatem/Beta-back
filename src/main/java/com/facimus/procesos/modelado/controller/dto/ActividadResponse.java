package com.facimus.procesos.modelado.controller.dto;

import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.TipoActividad;

public record ActividadResponse(Long id, String nombre, String descripcion, int posicionX, int posicionY,
        Long laneId, TipoActividad tipoActividad) {

    public static ActividadResponse of(Actividad a) {
        TipoActividad tipo = a.getTipoActividad() != null ? a.getTipoActividad() : TipoActividad.TAREA;
        return new ActividadResponse(a.getId(), a.getNombre(), a.getDescripcion(), a.getPosicionX(),
                a.getPosicionY(), a.getLane().getId(), tipo);
    }
}
