package com.facimus.procesos.gestion.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.common.api.PageResponse;
import com.facimus.procesos.gestion.controller.dto.RolProcesoConsultaResponse;
import com.facimus.procesos.gestion.controller.dto.RolProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.RolProcesoVistaResponse;
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.gestion.service.RolProcesoService;
import com.facimus.procesos.security.ApiPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/** HU-17 a HU-20: roles de proceso (solo administrador crea/edita/elimina). */
@Tag(name = "Roles de proceso", description = "Gestion de roles asignables a lanes")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolProcesoController {

    private final RolProcesoService rolProcesoService;

    @Operation(summary = "Listar roles con informacion de uso")
    @GetMapping
    public ResponseEntity<List<RolProcesoVistaResponse>> listar(@AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        List<RolProcesoVistaResponse> roles = rolProcesoService.listarConUso(empresaId).stream()
                .map(RolProcesoVistaResponse::of)
                .toList();
        return ResponseEntity.ok(roles);
    }

    @Operation(summary = "Buscar roles con paginacion")
    @GetMapping("/consulta")
    public ResponseEntity<PageResponse<RolProcesoConsultaResponse>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int pagina,
            @AuthenticationPrincipal ApiPrincipal principal) {
        final int tamano = 10;
        var page = rolProcesoService.buscarConProcesos(
                principal.empresaId(), nombre, PageRequest.of(pagina, tamano))
                .map(consulta -> RolProcesoConsultaResponse.of(consulta.rol(), consulta.procesos()));
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Operation(summary = "Crear rol de proceso")
    @ApiResponse(responseCode = "201", description = "Rol creado")
    @ApiResponse(responseCode = "409", description = "Nombre duplicado en la empresa")
    @PostMapping
    public ResponseEntity<RolProcesoVistaResponse> crear(@Validated @RequestBody RolProcesoRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        RolProceso rol = rolProcesoService.crear(empresaId, request.nombre(), request.descripcion());
        return ResponseEntity.created(URI.create("/api/v1/roles/" + rol.getId()))
                .body(new RolProcesoVistaResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), 0, false));
    }

    @Operation(summary = "Obtener rol por ID con conteo de usos")
    @GetMapping("/{id}")
    public ResponseEntity<RolProcesoVistaResponse> obtener(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        RolProceso rol = rolProcesoService.obtener(empresaId, id);
        long usos = rolProcesoService.contarUsos(empresaId, id);
        return ResponseEntity.ok(new RolProcesoVistaResponse(
                rol.getId(), rol.getNombre(), rol.getDescripcion(), usos, usos > 0));
    }

    @Operation(summary = "Editar rol de proceso")
    @PutMapping("/{id}")
    public ResponseEntity<RolProcesoVistaResponse> editar(@PathVariable Long id,
            @Validated @RequestBody RolProcesoRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        RolProceso rol = rolProcesoService.editar(empresaId, id, request.nombre(), request.descripcion());
        long usos = rolProcesoService.contarUsos(empresaId, id);
        return ResponseEntity.ok(
                new RolProcesoVistaResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), usos, usos > 0));
    }

    @Operation(summary = "Eliminar rol (baja logica)")
    @ApiResponse(responseCode = "204", description = "Rol eliminado")
    @ApiResponse(responseCode = "409", description = "Rol en uso, no se puede eliminar")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        rolProcesoService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
