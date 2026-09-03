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
import com.facimus.procesos.modelado.controller.dto.EditarPoolRequest;
import com.facimus.procesos.modelado.controller.dto.PoolRequest;
import com.facimus.procesos.modelado.controller.dto.PoolResponse;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.service.PoolService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-21 y HU-23: pools (participantes del proceso). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PoolController {

    private final PoolService poolService;

    @GetMapping("/procesos/{procesoId}/pools")
    public ResponseEntity<List<PoolResponse>> listar(@PathVariable Long procesoId, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        List<PoolResponse> pools = poolService.listarPorProceso(empresaId, procesoId).stream()
                .map(PoolResponse::of)
                .toList();
        return ResponseEntity.ok(pools);
    }

    @PostMapping("/procesos/{procesoId}/pools")
    public ResponseEntity<PoolResponse> crear(@PathVariable Long procesoId,
            @Validated @RequestBody PoolRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Pool pool = poolService.crear(empresaId, procesoId, request.nombre(), request.tipoParticipante(),
                request.cajaNegra());
        return ResponseEntity.status(HttpStatus.CREATED).body(PoolResponse.of(pool));
    }

    @PutMapping("/pools/{id}")
    public ResponseEntity<PoolResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EditarPoolRequest request, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Pool pool = poolService.editar(empresaId, id, request.nombre(), request.tipoParticipante());
        return ResponseEntity.ok(PoolResponse.of(pool));
    }

    @DeleteMapping("/pools/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        poolService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
