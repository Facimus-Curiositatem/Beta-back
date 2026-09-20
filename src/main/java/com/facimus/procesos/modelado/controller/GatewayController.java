package com.facimus.procesos.modelado.controller;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.modelado.controller.dto.GatewayRequest;
import com.facimus.procesos.modelado.controller.dto.GatewayResponse;
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

    @PostMapping("/lanes/{laneId}/gateways")
    public ResponseEntity<GatewayResponse> crear(@PathVariable Long laneId,
            @Validated @RequestBody GatewayRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.crear(empresaId, laneId, request.nombre(), request.tipoGateway(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.created(URI.create("/api/v1/gateways/" + gateway.getId()))
                .body(GatewayResponse.of(gateway));
    }

    @PutMapping("/gateways/{id}")
    public ResponseEntity<GatewayResponse> editar(@PathVariable Long id,
            @Validated @RequestBody GatewayRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Gateway gateway = gatewayService.editar(empresaId, id, request.nombre(), request.tipoGateway(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.ok(GatewayResponse.of(gateway));
    }

    @DeleteMapping("/gateways/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        gatewayService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
