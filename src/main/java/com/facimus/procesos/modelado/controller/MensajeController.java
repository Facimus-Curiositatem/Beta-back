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

import com.facimus.procesos.modelado.controller.dto.EditarMensajeRequest;
import com.facimus.procesos.modelado.controller.dto.MensajeRequest;
import com.facimus.procesos.modelado.controller.dto.MensajeResponse;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.service.MensajeService;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    @GetMapping("/procesos/{procesoId}/mensajes")
    public ResponseEntity<List<MensajeResponse>> listar(@PathVariable Long procesoId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(mensajeService.listarPorProceso(principal.empresaId(), procesoId).stream()
                .map(MensajeResponse::of).toList());
    }

    @GetMapping("/mensajes/{id}")
    public ResponseEntity<MensajeResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(MensajeResponse.of(mensajeService.obtener(principal.empresaId(), id)));
    }

    @PostMapping("/procesos/{procesoId}/mensajes")
    public ResponseEntity<MensajeResponse> crear(@PathVariable Long procesoId,
            @Validated @RequestBody MensajeRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Mensaje mensaje = request.eventoThrowId() == null
                ? mensajeService.crear(principal.empresaId(), procesoId, request.nombre(), request.contenido(),
                        request.poolOrigenId(), request.poolDestinoId())
                : mensajeService.crear(principal.empresaId(), procesoId, request.nombre(), request.contenido(),
                        request.poolOrigenId(), request.poolDestinoId(), request.eventoThrowId(), request.eventoCatchId(),
                        request.claveCorrelacion(), request.tipoDestinoExterno(), request.destinoExterno(),
                        request.politicaSinCaso());
        return ResponseEntity.created(URI.create("/api/v1/mensajes/" + mensaje.getId()))
                .body(MensajeResponse.of(mensaje));
    }

    @PutMapping("/mensajes/{id}")
    public ResponseEntity<MensajeResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EditarMensajeRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        boolean edicionLegacy = request.eventoCatchId() == null && request.claveCorrelacion() == null
                && request.tipoDestinoExterno() == null && request.destinoExterno() == null
                && request.politicaSinCaso() == null;
        Mensaje mensaje = edicionLegacy
                ? mensajeService.editar(principal.empresaId(), id, request.nombre(), request.contenido())
                : mensajeService.editar(principal.empresaId(), id, request.nombre(), request.contenido(),
                        request.eventoCatchId(), request.claveCorrelacion(), request.tipoDestinoExterno(),
                        request.destinoExterno(), request.politicaSinCaso());
        return ResponseEntity.ok(MensajeResponse.of(mensaje));
    }

    @DeleteMapping("/mensajes/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        mensajeService.eliminar(principal.empresaId(), id);
        return ResponseEntity.noContent().build();
    }
}
