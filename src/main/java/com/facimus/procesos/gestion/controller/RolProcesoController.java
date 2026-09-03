package com.facimus.procesos.gestion.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.common.AccesoProhibidoException;
import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.controller.dto.RolProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.RolProcesoVistaResponse;
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.gestion.service.RolProcesoService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-17 a HU-20: roles de proceso (solo administrador crea/edita/elimina). */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolProcesoController {

    private final RolProcesoService rolProcesoService;

    @GetMapping
    public ResponseEntity<List<RolProcesoVistaResponse>> listar(HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        List<RolProcesoVistaResponse> roles = rolProcesoService.listarConUso(empresaId).stream()
                .map(RolProcesoVistaResponse::of)
                .toList();
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    public ResponseEntity<RolProcesoVistaResponse> crear(@Validated @RequestBody RolProcesoRequest request,
            HttpSession session) {
        exigirAdministrador(session);
        Long empresaId = SesionActiva.empresaId(session);
        RolProceso rol = rolProcesoService.crear(empresaId, request.nombre(), request.descripcion());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RolProcesoVistaResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), 0, false));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolProcesoVistaResponse> obtener(@PathVariable Long id, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        return rolProcesoService.listarConUso(empresaId).stream()
                .filter(v -> v.rol().getId().equals(id))
                .map(RolProcesoVistaResponse::of)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    rolProcesoService.obtener(empresaId, id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolProcesoVistaResponse> editar(@PathVariable Long id,
            @Validated @RequestBody RolProcesoRequest request, HttpSession session) {
        exigirAdministrador(session);
        Long empresaId = SesionActiva.empresaId(session);
        RolProceso rol = rolProcesoService.editar(empresaId, id, request.nombre(), request.descripcion());
        return ResponseEntity.ok(
                new RolProcesoVistaResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), 0, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpSession session) {
        exigirAdministrador(session);
        Long empresaId = SesionActiva.empresaId(session);
        rolProcesoService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }

    private void exigirAdministrador(HttpSession session) {
        if (!SesionActiva.esAdministrador(session)) {
            throw new AccesoProhibidoException("Solo un administrador puede gestionar roles de proceso.");
        }
    }
}
