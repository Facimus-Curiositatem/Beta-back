package com.facimus.procesos.modelado.controller;

import java.net.URI;
import java.util.List;

import org.modelmapper.ModelMapper;
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

import lombok.RequiredArgsConstructor;

/** HU-14 a HU-16: gateways (puntos de decision). */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;
    private final ModelMapper modelMapper;

    @PostMapping("/lanes/{laneId}/gateways")
    public ResponseEntity<GatewayResponse> crear(@PathVariable Long laneId,
            @Validated @RequestBody GatewayRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.crear(empresaId, laneId, request.nombre(), request.tipoGateway(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.created(URI.create("/api/v1/gateways/" + gateway.getId()))
                .body(modelMapper.map(gateway, GatewayResponse.class));
    }

    @GetMapping("/gateways/{id}")
    public ResponseEntity<GatewayResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.obtener(empresaId, id);
        return ResponseEntity.ok(modelMapper.map(gateway, GatewayResponse.class));
    }

    @GetMapping("/lanes/{laneId}/gateways")
    public ResponseEntity<List<GatewayResponse>> listar(@PathVariable Long laneId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        List<GatewayResponse> gateways = gatewayService.listarPorLane(empresaId, laneId).stream()
                .map(g -> modelMapper.map(g, GatewayResponse.class)).toList();
        return ResponseEntity.ok(gateways);
    }

    @PutMapping("/gateways/{id}")
    public ResponseEntity<GatewayResponse> editar(@PathVariable Long id,
            @Validated @RequestBody GatewayRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.editar(empresaId, id, request.nombre(), request.tipoGateway(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.ok(modelMapper.map(gateway, GatewayResponse.class));
    }

    @GetMapping("/gateways/{id}/impacto-eliminacion")
    public ResponseEntity<ImpactoEliminacionResponse> impactoEliminacion(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(ImpactoEliminacionResponse.of(
                gatewayService.evaluarImpactoEliminacion(principal.empresaId(), id)));
    }

    @DeleteMapping("/gateways/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        gatewayService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
