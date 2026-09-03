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
import com.facimus.procesos.modelado.controller.dto.LaneRequest;
import com.facimus.procesos.modelado.controller.dto.LaneResponse;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.service.LaneService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-22 y HU-24: lanes (divisiones internas de un pool). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LaneController {

    private final LaneService laneService;

    @GetMapping("/pools/{poolId}/lanes")
    public ResponseEntity<List<LaneResponse>> listar(@PathVariable Long poolId, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        List<LaneResponse> lanes = laneService.listarPorPool(empresaId, poolId).stream()
                .map(LaneResponse::of)
                .toList();
        return ResponseEntity.ok(lanes);
    }

    @PostMapping("/pools/{poolId}/lanes")
    public ResponseEntity<LaneResponse> crear(@PathVariable Long poolId,
            @Validated @RequestBody LaneRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Lane lane = laneService.crear(empresaId, poolId, request.nombre(), request.rolProcesoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(LaneResponse.of(lane));
    }

    @PutMapping("/lanes/{id}")
    public ResponseEntity<LaneResponse> editar(@PathVariable Long id,
            @Validated @RequestBody LaneRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Lane lane = laneService.editar(empresaId, id, request.nombre(), request.rolProcesoId());
        return ResponseEntity.ok(LaneResponse.of(lane));
    }

    @DeleteMapping("/lanes/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        laneService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
