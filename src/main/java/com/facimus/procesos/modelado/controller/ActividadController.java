package com.facimus.procesos.modelado.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.modelado.controller.dto.ActividadRequest;
import com.facimus.procesos.modelado.controller.dto.ActividadResponse;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.service.ActividadService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-08 a HU-10: actividades (tareas del proceso). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;

    @PostMapping("/lanes/{laneId}/actividades")
    public ResponseEntity<ActividadResponse> crear(@PathVariable Long laneId,
            @Validated @RequestBody ActividadRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Actividad actividad = actividadService.crear(empresaId, laneId, request.nombre(), request.descripcion(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.status(HttpStatus.CREATED).body(ActividadResponse.of(actividad));
    }

    @PutMapping("/actividades/{id}")
    public ResponseEntity<ActividadResponse> editar(@PathVariable Long id,
            @Validated @RequestBody ActividadRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Actividad actividad = actividadService.editar(empresaId, id, request.nombre(), request.descripcion(),
                request.posicionX(), request.posicionY());
        return ResponseEntity.ok(ActividadResponse.of(actividad));
    }

    @DeleteMapping("/actividades/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        actividadService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
