package com.facimus.procesos.gestion.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.common.AccesoProhibidoException;
import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.controller.dto.EditarProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.HistorialCambioResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoDetalleResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoRequest;
import com.facimus.procesos.gestion.controller.dto.ProcesoResponse;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.service.HistorialCambioService;
import com.facimus.procesos.gestion.service.ProcesoService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-04 a HU-07: creacion, edicion, eliminacion logica y consulta de procesos. */
@RestController
@RequestMapping("/api/procesos")
@RequiredArgsConstructor
public class ProcesoController {

    private static final int TAMANO_PAGINA = 10;

    private final ProcesoService procesoService;
    private final HistorialCambioService historialCambioService;

    @GetMapping
    public ResponseEntity<Page<ProcesoResponse>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoProceso estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int pagina,
            HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Page<ProcesoResponse> procesos = procesoService.buscar(empresaId, nombre, estado, categoria,
                        PageRequest.of(pagina, TAMANO_PAGINA, Sort.by("fechaModificacion").descending()))
                .map(ProcesoResponse::of);
        return ResponseEntity.ok(procesos);
    }

    @PostMapping
    public ResponseEntity<ProcesoResponse> crear(@Validated @RequestBody ProcesoRequest request, HttpSession session) {
        exigirEditor(session);
        Long empresaId = SesionActiva.empresaId(session);
        Long usuarioId = SesionActiva.usuarioId(session);
        Proceso proceso = procesoService.crear(empresaId, usuarioId, request.nombre(), request.descripcion(),
                request.categoria());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProcesoResponse.of(proceso));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcesoDetalleResponse> detalle(@PathVariable Long id, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Proceso proceso = procesoService.obtener(empresaId, id);
        List<HistorialCambioResponse> historial = historialCambioService.listarPorProceso(empresaId, id).stream()
                .map(HistorialCambioResponse::of)
                .toList();
        return ResponseEntity.ok(new ProcesoDetalleResponse(ProcesoResponse.of(proceso), historial));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcesoResponse> editar(@PathVariable Long id,
            @Validated @RequestBody EditarProcesoRequest request, HttpSession session) {
        exigirEditor(session);
        Long empresaId = SesionActiva.empresaId(session);
        Long usuarioId = SesionActiva.usuarioId(session);
        Proceso proceso = procesoService.editar(empresaId, id, usuarioId, request.nombre(), request.descripcion(),
                request.categoria(), request.estado());
        return ResponseEntity.ok(ProcesoResponse.of(proceso));
    }

    @PostMapping("/{id}/publicar")
    public ResponseEntity<ProcesoResponse> publicar(@PathVariable Long id, HttpSession session) {
        exigirEditor(session);
        Long empresaId = SesionActiva.empresaId(session);
        Long usuarioId = SesionActiva.usuarioId(session);
        Proceso proceso = procesoService.publicar(empresaId, id, usuarioId);
        return ResponseEntity.ok(ProcesoResponse.of(proceso));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpSession session) {
        if (!SesionActiva.esAdministrador(session)) {
            throw new AccesoProhibidoException("Solo un administrador puede eliminar procesos.");
        }
        Long empresaId = SesionActiva.empresaId(session);
        Long usuarioId = SesionActiva.usuarioId(session);
        procesoService.eliminarLogico(empresaId, id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    private void exigirEditor(HttpSession session) {
        if (!SesionActiva.puedeEditar(session)) {
            throw new AccesoProhibidoException("No tienes permisos para modificar procesos.");
        }
    }
}
