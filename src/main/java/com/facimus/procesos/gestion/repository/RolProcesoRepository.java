package com.facimus.procesos.gestion.repository;

import java.util.List;
import java.util.Optional;

import com.facimus.procesos.common.RepositorioTenant;
import com.facimus.procesos.gestion.model.RolProceso;

public interface RolProcesoRepository extends RepositorioTenant<RolProceso> {

    List<RolProceso> findAllByEmpresaIdAndActivoTrue(Long empresaId);

    boolean existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(Long empresaId, String nombre);

    Optional<RolProceso> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);
}
