package com.facimus.procesos.modelado.controller;

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

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.modelado.controller.dto.EditarMensajeRequest;
import com.facimus.procesos.modelado.controller.dto.MensajeRequest;
import com.facimus.procesos.modelado.controller.dto.MensajeResponse;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.service.MensajeService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-25 a HU-27: mensajes (comunicacion entre pools). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    @GetMapping("/procesos/{procesoId}/mensajes")
    public ResponseEntity<List<MensajeResponse>> listar(@PathVariable Long procesoId, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        List<MensajeResponse> mensajes = mensajeService.listarPorProceso(empresaId, procesoId).stream()
                .map(MensajeResponse::of)
                .toList();
        return ResponseEntity.ok(mensajes);
    }

    @PostMapping("/procesos/{procesoId}/mensajes")
    public ResponseEntity<MensajeResponse> crear(@PathVariable Long procesoId,
            @Validated @RequestBody MensajeRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Mensaje mensaje = mensajeService.crear(empresaId, procesoId, request.nombre(), request.contenido(),
                request.poolOrigenId(), request.poolDestinoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MensajeResponse.of(mensaje));
    }

    @PutMapping("/mensajes/{id}")
    public ResponseEntity<MensajeResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EditarMensajeRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Mensaje mensaje = mensajeService.editar(empresaId, id, request.nombre(), request.contenido());
        return ResponseEntity.ok(MensajeResponse.of(mensaje));
    }

    @DeleteMapping("/mensajes/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        mensajeService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
