package com.facimus.procesos.gestion.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.gestion.controller.dto.CompartirProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.ProcesoCompartidoDetalleResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoCompartidoResponse;
import com.facimus.procesos.gestion.model.ProcesoCompartido;
import com.facimus.procesos.gestion.service.ProcesoCompartidoService;
import com.facimus.procesos.security.ApiPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/** HU-23: procesos compartidos entre empresas en modo solo lectura. */
@Tag(name = "Procesos compartidos", description = "Compartir procesos en modo solo lectura entre empresas")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProcesoCompartidoController {

    private final ProcesoCompartidoService procesoCompartidoService;

    @Operation(summary = "Listar empresas con acceso compartido al proceso")
    @GetMapping("/procesos/{procesoId}/compartidos")
    public ResponseEntity<List<ProcesoCompartidoResponse>> listarCompartidos(@PathVariable Long procesoId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(procesoCompartidoService
                .listarCompartidosPorPropietario(principal.empresaId(), procesoId).stream()
                .map(ProcesoCompartidoResponse::of).toList());
    }

    @Operation(summary = "Compartir proceso con otra empresa")
    @ApiResponse(responseCode = "201", description = "Proceso compartido")
    @PostMapping("/procesos/{procesoId}/compartidos")
    public ResponseEntity<ProcesoCompartidoResponse> compartir(@PathVariable Long procesoId,
            @Validated @RequestBody CompartirProcesoRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        ProcesoCompartido compartido = procesoCompartidoService.compartir(principal.empresaId(),
                principal.usuarioId(), procesoId, request.empresaInvitadaId());
        return ResponseEntity.created(URI.create("/api/v1/procesos-compartidos/" + procesoId))
                .body(ProcesoCompartidoResponse.of(compartido));
    }

    @Operation(summary = "Retirar acceso compartido a una empresa")
    @ApiResponse(responseCode = "204", description = "Acceso retirado")
    @DeleteMapping("/procesos/{procesoId}/compartidos/{empresaInvitadaId}")
    public ResponseEntity<Void> dejarDeCompartir(@PathVariable Long procesoId,
            @PathVariable Long empresaInvitadaId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        procesoCompartidoService.dejarDeCompartir(principal.empresaId(), principal.usuarioId(),
                procesoId, empresaInvitadaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar procesos compartidos recibidos")
    @GetMapping("/procesos-compartidos")
    public ResponseEntity<List<ProcesoCompartidoResponse>> listarRecibidos(
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(procesoCompartidoService.listarRecibidos(principal.empresaId()).stream()
                .map(ProcesoCompartidoResponse::of).toList());
    }

    @Operation(summary = "Ver detalle de un proceso compartido (solo lectura)")
    @GetMapping("/procesos-compartidos/{procesoId}")
    public ResponseEntity<ProcesoCompartidoDetalleResponse> detalleCompartido(@PathVariable Long procesoId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(ProcesoCompartidoDetalleResponse.of(
                procesoCompartidoService.obtenerCompartido(principal.empresaId(), procesoId)));
    }
}
