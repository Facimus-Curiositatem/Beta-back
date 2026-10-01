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

import com.facimus.procesos.modelado.controller.dto.GatewayRequest;
import com.facimus.procesos.modelado.controller.dto.GatewayResponse;
import com.facimus.procesos.modelado.controller.dto.ImpactoEliminacionResponse;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.service.GatewayService;
import com.facimus.procesos.security.ApiPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/** HU-14 a HU-16: gateways (puntos de decision). */
@Tag(name = "Gateways", description = "Gestion de gateways (compuertas) de decision y bifurcacion")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;

    @Operation(summary = "Crear gateway en una lane")
    @ApiResponse(responseCode = "201", description = "Gateway creado")
    @PostMapping("/lanes/{laneId}/gateways")
    public ResponseEntity<GatewayResponse> crear(@PathVariable Long laneId,
            @Validated @RequestBody GatewayRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.crear(empresaId, laneId, request.nombre(), request.tipoGateway(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.created(URI.create("/api/v1/gateways/" + gateway.getId()))
                .body(GatewayResponse.of(gateway));
    }

    @Operation(summary = "Obtener detalle de un gateway")
    @GetMapping("/gateways/{id}")
    public ResponseEntity<GatewayResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.obtener(empresaId, id);
        return ResponseEntity.ok(GatewayResponse.of(gateway));
    }

    @Operation(summary = "Listar gateways de una lane")
    @GetMapping("/lanes/{laneId}/gateways")
    public ResponseEntity<List<GatewayResponse>> listar(@PathVariable Long laneId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        List<GatewayResponse> gateways = gatewayService.listarPorLane(empresaId, laneId).stream()
                .map(GatewayResponse::of).toList();
        return ResponseEntity.ok(gateways);
    }

    @Operation(summary = "Editar gateway")
    @PutMapping("/gateways/{id}")
    public ResponseEntity<GatewayResponse> editar(@PathVariable Long id,
            @Validated @RequestBody GatewayRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.editar(empresaId, id, request.nombre(), request.tipoGateway(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.ok(GatewayResponse.of(gateway));
    }

    @Operation(summary = "Evaluar impacto de eliminar un gateway")
    @GetMapping("/gateways/{id}/impacto-eliminacion")
    public ResponseEntity<ImpactoEliminacionResponse> impactoEliminacion(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(ImpactoEliminacionResponse.of(
                gatewayService.evaluarImpactoEliminacion(principal.empresaId(), id)));
    }

    @Operation(summary = "Eliminar gateway (baja logica)")
    @ApiResponse(responseCode = "204", description = "Gateway eliminado")
    @DeleteMapping("/gateways/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        gatewayService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
