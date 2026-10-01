package com.facimus.procesos.modelado.controller;

import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.modelado.controller.dto.ActividadRequest;
import com.facimus.procesos.modelado.controller.dto.ActividadResponse;
import com.facimus.procesos.modelado.controller.dto.ImpactoEliminacionResponse;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.service.ActividadService;
import com.facimus.procesos.security.ApiPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/** HU-08 a HU-10: actividades (tareas del proceso). */
@Tag(name = "Actividades", description = "Gestion de actividades (tareas) en el modelado BPMN")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;

    @Operation(summary = "Crear actividad en una lane")
    @ApiResponse(responseCode = "201", description = "Actividad creada")
    @PostMapping("/lanes/{laneId}/actividades")
    public ResponseEntity<ActividadResponse> crear(@PathVariable Long laneId,
            @Validated @RequestBody ActividadRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Actividad actividad = request.tipoActividad() == null
                ? actividadService.crear(empresaId, laneId, request.nombre(), request.descripcion(),
                        request.posicionX(), request.posicionY())
                : actividadService.crear(empresaId, laneId, request.nombre(), request.descripcion(),
                        request.posicionX(), request.posicionY(), request.tipoActividad());
        return ResponseEntity.created(URI.create("/api/v1/actividades/" + actividad.getId()))
                .body(ActividadResponse.of(actividad));
    }

    @Operation(summary = "Obtener detalle de una actividad")
    @GetMapping("/actividades/{id}")
    public ResponseEntity<ActividadResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(ActividadResponse.of(actividadService.obtener(principal.empresaId(), id)));
    }

    @Operation(summary = "Listar actividades de una lane")
    @GetMapping("/lanes/{laneId}/actividades")
    public ResponseEntity<List<ActividadResponse>> listar(@PathVariable Long laneId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(actividadService.listarPorLane(principal.empresaId(), laneId).stream()
                .map(ActividadResponse::of).toList());
    }

    @Operation(summary = "Editar actividad")
    @PutMapping("/actividades/{id}")
    public ResponseEntity<ActividadResponse> editar(@PathVariable Long id,
            @Validated @RequestBody ActividadRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Actividad actividad = request.tipoActividad() == null
                ? actividadService.editar(empresaId, id, request.nombre(), request.descripcion(),
                        request.posicionX(), request.posicionY(), request.laneId())
                : actividadService.editar(empresaId, id, request.nombre(), request.descripcion(),
                        request.posicionX(), request.posicionY(), request.laneId(), request.tipoActividad());
        return ResponseEntity.ok(ActividadResponse.of(actividad));
    }

    @Operation(summary = "Evaluar impacto de eliminar una actividad")
    @GetMapping("/actividades/{id}/impacto-eliminacion")
    public ResponseEntity<ImpactoEliminacionResponse> impactoEliminacion(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(ImpactoEliminacionResponse.of(
                actividadService.evaluarImpactoEliminacion(principal.empresaId(), id)));
    }

    @Operation(summary = "Eliminar actividad (baja logica)")
    @ApiResponse(responseCode = "204", description = "Actividad eliminada")
    @DeleteMapping("/actividades/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        actividadService.eliminar(principal.empresaId(), id);
        return ResponseEntity.noContent().build();
    }
}
