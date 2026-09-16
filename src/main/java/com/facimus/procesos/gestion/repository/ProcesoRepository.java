package com.facimus.procesos.gestion.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.facimus.procesos.common.RepositorioTenant;
import com.facimus.procesos.gestion.model.Proceso;

public interface ProcesoRepository extends RepositorioTenant<Proceso>, JpaSpecificationExecutor<Proceso> {

    boolean existsByEmpresaIdAndNombreIgnoreCaseAndActivoTrue(Long empresaId, String nombre);
}
