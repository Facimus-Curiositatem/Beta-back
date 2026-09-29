package com.facimus.procesos.gestion.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.common.api.PageResponse;
import com.facimus.procesos.gestion.controller.dto.CambiarEstadoProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.EditarProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.HistorialCambioResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoDetalleResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoDiagramaResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.ProcesoResponse;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.service.HistorialCambioService;
import com.facimus.procesos.gestion.service.ProcesoDiagramaService;
import com.facimus.procesos.gestion.service.ProcesoService;
import com.facimus.procesos.modelado.controller.dto.ActividadResponse;
import com.facimus.procesos.modelado.controller.dto.ArcoResponse;
import com.facimus.procesos.modelado.controller.dto.EventoMensajeResponse;
import com.facimus.procesos.modelado.controller.dto.GatewayResponse;
import com.facimus.procesos.modelado.controller.dto.LaneResponse;
import com.facimus.procesos.modelado.controller.dto.MensajeResponse;
import com.facimus.procesos.modelado.controller.dto.PoolResponse;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;

/** HU-04 a HU-07: creacion, edicion, eliminacion logica y consulta de procesos. */
@RestController
@RequestMapping("/api/v1/procesos")
@RequiredArgsConstructor
public class ProcesoController {

    private static final int TAMANO_PAGINA = 10;

    private final ProcesoService procesoService;
    private final HistorialCambioService historialCambioService;
    private final ProcesoDiagramaService procesoDiagramaService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<PageResponse<ProcesoResponse>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoProceso estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa.") int pagina,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Page<ProcesoResponse> procesos = procesoService.buscar(empresaId, nombre, estado, categoria, activo,
                        PageRequest.of(pagina, TAMANO_PAGINA, Sort.by("fechaModificacion").descending()))
                .map(p -> modelMapper.map(p, ProcesoResponse.class));
        return ResponseEntity.ok(PageResponse.from(procesos));
    }

    @PostMapping
    public ResponseEntity<ProcesoResponse> crear(@Validated @RequestBody ProcesoRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Long usuarioId = principal.usuarioId();
        Proceso proceso = procesoService.crear(empresaId, usuarioId, request.nombre(), request.descripcion(),
                request.categoria());
        return ResponseEntity.created(URI.create("/api/v1/procesos/" + proceso.getId()))
                .body(modelMapper.map(proceso, ProcesoResponse.class));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcesoDetalleResponse> detalle(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Proceso proceso = procesoService.obtener(empresaId, id);
        List<HistorialCambioResponse> historial = historialCambioService.listarPorProceso(empresaId, id).stream()
                .map(h -> modelMapper.map(h, HistorialCambioResponse.class))
                .toList();
        return ResponseEntity.ok(new ProcesoDetalleResponse(
                modelMapper.map(proceso, ProcesoResponse.class), historial));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcesoResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EditarProcesoRequest request, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Long usuarioId = principal.usuarioId();
        Proceso proceso = procesoService.editarDatos(empresaId, id, usuarioId, request.nombre(), request.descripcion(),
                request.categoria());
        return ResponseEntity.ok(modelMapper.map(proceso, ProcesoResponse.class));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProcesoResponse> cambiarEstado(@PathVariable Long id,
            @Validated @RequestBody CambiarEstadoProcesoRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Long usuarioId = principal.usuarioId();
        Proceso proceso = procesoService.cambiarEstado(empresaId, id, usuarioId, request.estado());
        return ResponseEntity.ok(modelMapper.map(proceso, ProcesoResponse.class));
    }

    @GetMapping("/{id}/diagrama")
    public ResponseEntity<ProcesoDiagramaResponse> diagrama(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        var diagrama = procesoDiagramaService.obtener(principal.empresaId(), id);
        return ResponseEntity.ok(new ProcesoDiagramaResponse(
                modelMapper.map(diagrama.proceso(), ProcesoResponse.class),
                diagrama.pools().stream().map(p -> modelMapper.map(p, PoolResponse.class)).toList(),
                diagrama.lanes().stream().map(l -> modelMapper.map(l, LaneResponse.class)).toList(),
                diagrama.actividades().stream().map(a -> modelMapper.map(a, ActividadResponse.class)).toList(),
                diagrama.gateways().stream().map(g -> modelMapper.map(g, GatewayResponse.class)).toList(),
                diagrama.eventos().stream().map(e -> modelMapper.map(e, EventoMensajeResponse.class)).toList(),
                diagrama.arcos().stream().map(a -> modelMapper.map(a, ArcoResponse.class)).toList(),
                diagrama.mensajes().stream().map(m -> modelMapper.map(m, MensajeResponse.class)).toList()));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialCambioResponse>> historial(@PathVariable Long id,
            @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        procesoService.obtener(empresaId, id);
        return ResponseEntity.ok(historialCambioService.listarPorProceso(empresaId, id).stream()
                .map(h -> modelMapper.map(h, HistorialCambioResponse.class))
                .toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal ApiPrincipal principal) {
        Long empresaId = principal.empresaId();
        Long usuarioId = principal.usuarioId();
        procesoService.eliminarLogico(empresaId, id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
