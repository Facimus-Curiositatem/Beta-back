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

import org.modelmapper.ModelMapper;

import com.facimus.procesos.modelado.controller.dto.EditarPoolRequest;
import com.facimus.procesos.modelado.controller.dto.PoolRequest;
import com.facimus.procesos.modelado.controller.dto.PoolResponse;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.gestion.service.OperacionEstructura;
import com.facimus.procesos.gestion.service.PermisoEstructuraService;
import com.facimus.procesos.modelado.service.PoolService;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PoolController {

    private final PoolService poolService;
    private final PermisoEstructuraService permisoEstructuraService;
    private final ModelMapper modelMapper;

    @GetMapping("/procesos/{procesoId}/pools")
    public ResponseEntity<List<PoolResponse>> listar(@PathVariable Long procesoId,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(poolService.listarPorProceso(principal.empresaId(), procesoId).stream()
                .map(p -> modelMapper.map(p, PoolResponse.class)).toList());
    }

    @GetMapping("/pools/{id}")
    public ResponseEntity<PoolResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        return ResponseEntity.ok(modelMapper.map(poolService.obtener(principal.empresaId(), id), PoolResponse.class));
    }

    @PostMapping("/procesos/{procesoId}/pools")
    public ResponseEntity<PoolResponse> crear(@PathVariable Long procesoId,
            @Validated @RequestBody PoolRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        permisoEstructuraService.validar(principal.empresaId(), principal.rol(), OperacionEstructura.CREAR_POOL);
        Pool pool = poolService.crear(principal.empresaId(), procesoId, request.nombre(),
                request.tipoParticipante(), request.cajaNegra());
        return ResponseEntity.created(URI.create("/api/v1/pools/" + pool.getId()))
                .body(modelMapper.map(pool, PoolResponse.class));
    }

    @PutMapping("/pools/{id}")
    public ResponseEntity<PoolResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EditarPoolRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        permisoEstructuraService.validar(empresaId, principal.rol(), OperacionEstructura.EDITAR_POOL);
        Pool pool = request.cajaNegra() == null
                ? poolService.editar(empresaId, id, request.nombre(), request.tipoParticipante())
                : poolService.editar(empresaId, id, request.nombre(), request.tipoParticipante(), request.cajaNegra());
        return ResponseEntity.ok(modelMapper.map(pool, PoolResponse.class));
    }

    @DeleteMapping("/pools/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        permisoEstructuraService.validar(principal.empresaId(), principal.rol(), OperacionEstructura.ELIMINAR_POOL);
        poolService.eliminar(principal.empresaId(), id);
        return ResponseEntity.noContent().build();
    }
}
