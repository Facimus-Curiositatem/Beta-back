package com.facimus.procesos.modelado.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.security.ApiPrincipal;

/** Reglas de edicion configurables para la estructura de un pool (HU-24). */
@Service
public class PermisoPoolService {

    public void validarEdicion(Pool pool) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ApiPrincipal principal)) {
            return;
        }
        if (!pool.getEmpresa().getId().equals(principal.empresaId())
                || !pool.getRolesEdicion().contains(principal.rol())) {
            throw new AccessDeniedException("Su rol no tiene permiso para modificar este pool.");
        }
    }

    public void validarConfiguracion() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ApiPrincipal principal
                && principal.rol() != RolAcceso.ADMINISTRADOR) {
            throw new AccessDeniedException("Solo un administrador puede configurar los permisos del pool.");
        }
    }
}
