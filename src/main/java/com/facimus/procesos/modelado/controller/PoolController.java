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

import com.facimus.procesos.modelado.controller.dto.EditarPoolRequest;
import com.facimus.procesos.modelado.controller.dto.PoolRequest;
import com.facimus.procesos.modelado.controller.dto.PoolResponse;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.service.PoolService;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PoolController {

    private final PoolService poolService;

    @GetMapping("/procesos/{procesoId}/pools")
    public ResponseEntity<List<PoolResponse>> listar(@PathVariable Long procesoId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(poolService.listarPorProceso(principal.empresaId(), procesoId).stream()
                .map(PoolResponse::of).toList());
    }

    @GetMapping("/pools/{id}")
    public ResponseEntity<PoolResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(PoolResponse.of(poolService.obtener(principal.empresaId(), id)));
    }

    @PostMapping("/procesos/{procesoId}/pools")
    public ResponseEntity<PoolResponse> crear(@PathVariable Long procesoId,
            @Validated @RequestBody PoolRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Pool pool = poolService.crear(principal.empresaId(), procesoId, request.nombre(),
                request.tipoParticipante(), request.cajaNegra());
        return ResponseEntity.created(URI.create("/api/v1/pools/" + pool.getId()))
                .body(PoolResponse.of(pool));
    }

    @PutMapping("/pools/{id}")
    public ResponseEntity<PoolResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EditarPoolRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Pool pool = request.cajaNegra() == null
                ? poolService.editar(empresaId, id, request.nombre(), request.tipoParticipante())
                : poolService.editar(empresaId, id, request.nombre(), request.tipoParticipante(), request.cajaNegra());
        return ResponseEntity.ok(PoolResponse.of(pool));
    }

    @DeleteMapping("/pools/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        poolService.eliminar(principal.empresaId(), id);
        return ResponseEntity.noContent().build();
    }
}
