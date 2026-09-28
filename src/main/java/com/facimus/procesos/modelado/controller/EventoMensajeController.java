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

import com.facimus.procesos.modelado.controller.dto.EventoMensajeRequest;
import com.facimus.procesos.modelado.controller.dto.EventoMensajeResponse;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.service.EventoMensajeService;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EventoMensajeController {

    private final EventoMensajeService eventoMensajeService;

    @PostMapping("/lanes/{laneId}/eventos-mensaje")
    public ResponseEntity<EventoMensajeResponse> crear(@PathVariable Long laneId,
            @Validated @RequestBody EventoMensajeRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        EventoMensaje evento = eventoMensajeService.crear(principal.empresaId(), laneId, request.nombre(),
                request.tipoEvento(), request.contenido(), request.claveCorrelacion(),
                request.posicionX(), request.posicionY(), request.origenExterno());
        return ResponseEntity.created(URI.create("/api/v1/eventos-mensaje/" + evento.getId()))
                .body(EventoMensajeResponse.of(evento));
    }

    @GetMapping("/eventos-mensaje/{id}")
    public ResponseEntity<EventoMensajeResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(EventoMensajeResponse.of(eventoMensajeService.obtener(principal.empresaId(), id)));
    }

    @GetMapping("/lanes/{laneId}/eventos-mensaje")
    public ResponseEntity<List<EventoMensajeResponse>> listar(@PathVariable Long laneId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(eventoMensajeService.listarPorLane(principal.empresaId(), laneId).stream()
                .map(EventoMensajeResponse::of).toList());
    }

    @PutMapping("/eventos-mensaje/{id}")
    public ResponseEntity<EventoMensajeResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EventoMensajeRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        EventoMensaje evento = eventoMensajeService.editar(principal.empresaId(), id, request.nombre(),
                request.tipoEvento(), request.contenido(), request.claveCorrelacion(),
                request.posicionX(), request.posicionY(), request.origenExterno());
        return ResponseEntity.ok(EventoMensajeResponse.of(evento));
    }

    @DeleteMapping("/eventos-mensaje/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        eventoMensajeService.eliminar(principal.empresaId(), id);
        return ResponseEntity.noContent().build();
    }
}
