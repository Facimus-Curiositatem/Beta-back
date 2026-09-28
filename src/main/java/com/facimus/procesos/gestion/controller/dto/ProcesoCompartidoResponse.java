package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.model.ProcesoCompartido;

public record ProcesoCompartidoResponse(Long id, Long procesoId, String procesoNombre,
        Long empresaPropietariaId, String empresaPropietaria,
        Long empresaInvitadaId, String empresaInvitada, boolean soloLectura) {

    public static ProcesoCompartidoResponse of(ProcesoCompartido compartido) {
        return new ProcesoCompartidoResponse(compartido.getId(), compartido.getProceso().getId(),
                compartido.getProceso().getNombre(), compartido.getEmpresa().getId(),
                compartido.getEmpresa().getNombre(), compartido.getEmpresaInvitada().getId(),
                compartido.getEmpresaInvitada().getNombre(), compartido.isSoloLectura());
    }
}
