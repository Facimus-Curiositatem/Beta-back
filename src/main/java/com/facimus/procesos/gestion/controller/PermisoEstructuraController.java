package com.facimus.procesos.gestion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.gestion.controller.dto.PermisoEstructuraRequest;
import com.facimus.procesos.gestion.controller.dto.PermisoEstructuraResponse;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.service.PermisoEstructuraService;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;

/** HU-24: configuracion de permisos de estructura por rol de acceso. */
@RestController
@RequestMapping("/api/v1/permisos-estructura")
@RequiredArgsConstructor
public class PermisoEstructuraController {

    private final PermisoEstructuraService permisoEstructuraService;

    @GetMapping
    public ResponseEntity<List<PermisoEstructuraResponse>> listar(@AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(permisoEstructuraService.listar(principal.empresaId()).stream()
                .map(PermisoEstructuraResponse::of).toList());
    }

    @PutMapping("/{rol}")
    public ResponseEntity<PermisoEstructuraResponse> actualizar(@PathVariable RolAcceso rol,
            @RequestBody PermisoEstructuraRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        var permiso = permisoEstructuraService.actualizar(principal.empresaId(), rol,
                request.crearPool(), request.editarPool(), request.eliminarPool(),
                request.crearLane(), request.editarLane(), request.eliminarLane());
        return ResponseEntity.ok(PermisoEstructuraResponse.of(permiso));
    }
}
