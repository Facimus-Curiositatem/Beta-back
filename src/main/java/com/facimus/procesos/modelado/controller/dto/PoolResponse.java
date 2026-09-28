package com.facimus.procesos.modelado.controller.dto;

import java.util.Set;

import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoParticipante;

public record PoolResponse(Long id, String nombre, TipoParticipante tipoParticipante, boolean cajaNegra,
        int orden, Long procesoId, Set<RolAcceso> rolesEdicion) {

    public static PoolResponse of(Pool pool) {
        return new PoolResponse(pool.getId(), pool.getNombre(), pool.getTipoParticipante(), pool.isCajaNegra(),
                pool.getOrden(), pool.getProceso().getId(), pool.getRolesEdicion());
    }
}
