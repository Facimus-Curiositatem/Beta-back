package com.facimus.procesos.modelado.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.repository.UsuarioRepository;
import com.facimus.procesos.gestion.service.HistorialCambioService;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;

/** Registra en la bitacora los cambios realizados sobre el diagrama. */
@Service
@RequiredArgsConstructor
public class AuditoriaModeladoService {

    private final UsuarioRepository usuarioRepository;
    private final HistorialCambioService historialCambioService;

    public void registrar(Proceso proceso, String descripcion) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ApiPrincipal principal)) {
            return;
        }
        Long empresaId = proceso.getEmpresa().getId();
        if (!empresaId.equals(principal.empresaId())) {
            return;
        }
        usuarioRepository.findByIdAndEmpresaId(principal.usuarioId(), empresaId)
                .ifPresent(usuario -> historialCambioService.registrar(proceso, usuario, descripcion));
    }
}
