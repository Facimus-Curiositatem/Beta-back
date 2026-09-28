package com.facimus.procesos.gestion.repository;

import java.util.List;
import java.util.Optional;

import com.facimus.procesos.common.RepositorioTenant;
import com.facimus.procesos.gestion.model.ProcesoCompartido;

public interface ProcesoCompartidoRepository extends RepositorioTenant<ProcesoCompartido> {

    List<ProcesoCompartido> findAllByProcesoIdAndEmpresaIdAndActivoTrue(Long procesoId, Long empresaId);

    List<ProcesoCompartido> findAllByEmpresaInvitadaIdAndActivoTrue(Long empresaInvitadaId);

    Optional<ProcesoCompartido> findByProcesoIdAndEmpresaInvitadaIdAndActivoTrue(Long procesoId,
            Long empresaInvitadaId);

    Optional<ProcesoCompartido> findByProcesoIdAndEmpresaIdAndEmpresaInvitadaId(Long procesoId,
            Long empresaId, Long empresaInvitadaId);
}
