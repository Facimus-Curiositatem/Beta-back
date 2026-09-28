package com.facimus.procesos.modelado.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.TipoActividad;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

import lombok.RequiredArgsConstructor;

/** HU-08 a HU-10: actividades (tareas del proceso). */
@Service
@RequiredArgsConstructor
public class ActividadService {

    private final NodoFlujoRepository nodoFlujoRepository;
    private final LaneRepository laneRepository;
    private final ArcoRepository arcoRepository;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public Actividad crear(Long empresaId, Long laneId, String nombre, String descripcion, int posX, int posY) {
        return crear(empresaId, laneId, nombre, descripcion, posX, posY, TipoActividad.TAREA);
    }

    @Transactional
    public Actividad crear(Long empresaId, Long laneId, String nombre, String descripcion, int posX, int posY,
            TipoActividad tipoActividad) {
        Lane lane = laneRepository.findByIdAndEmpresaId(laneId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada."));
        if (lane.getPool().isCajaNegra()) {
            throw new ReglaNegocioException("Un pool de caja negra no puede contener actividades.");
        }
        Long procesoId = lane.getPool().getProceso().getId();
        if (nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(nombre, procesoId, empresaId)) {
            throw new ReglaNegocioException("Ya existe un nodo con el nombre \"" + nombre + "\" en este proceso.");
        }

        Actividad actividad = new Actividad();
        actividad.setEmpresa(lane.getEmpresa());
        actividad.setLane(lane);
        actividad.setNombre(nombre);
        actividad.setDescripcion(descripcion);
        actividad.setTipoActividad(tipoActividad != null ? tipoActividad : TipoActividad.TAREA);
        actividad.setPosicionX(posX);
        actividad.setPosicionY(posY);
        actividad = (Actividad) nodoFlujoRepository.save(actividad);
        auditoriaModeladoService.registrar(lane.getPool().getProceso(), "Actividad creada: " + actividad.getNombre() + ".");
        return actividad;
    }

    @Transactional
    public Actividad editar(Long empresaId, Long actividadId, String nombre, String descripcion, int posX, int posY,
            Long laneId) {
        return editar(empresaId, actividadId, nombre, descripcion, posX, posY, laneId, null);
    }

    @Transactional
    public Actividad editar(Long empresaId, Long actividadId, String nombre, String descripcion, int posX, int posY,
            Long laneId, TipoActividad tipoActividad) {
        Actividad actividad = obtener(empresaId, actividadId);
        if (laneId != null && !laneId.equals(actividad.getLane().getId())) {
            Lane nuevoLane = laneRepository.findByIdAndEmpresaId(laneId, empresaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada."));
            if (nuevoLane.getPool().isCajaNegra()) {
                throw new ReglaNegocioException("Un pool de caja negra no puede contener actividades.");
            }
            Long procesoActual = actividad.getLane().getPool().getProceso().getId();
            Long procesoNuevo = nuevoLane.getPool().getProceso().getId();
            if (!procesoActual.equals(procesoNuevo)) {
                throw new ReglaNegocioException("El lane destino debe pertenecer al mismo proceso.");
            }
            Long poolActual = actividad.getLane().getPool().getId();
            Long poolNuevo = nuevoLane.getPool().getId();
            if (!poolActual.equals(poolNuevo)) {
                boolean tieneArcos = !arcoRepository.findAllByOrigenIdAndEmpresaId(actividadId, empresaId).isEmpty()
                        || !arcoRepository.findAllByDestinoIdAndEmpresaId(actividadId, empresaId).isEmpty();
                if (tieneArcos) {
                    throw new ReglaNegocioException(
                            "No se puede mover la actividad a otro pool porque tiene arcos conectados.");
                }
            }
            actividad.setLane(nuevoLane);
        }
        Long procesoId = actividad.getLane().getPool().getProceso().getId();
        if (!actividad.getNombre().equalsIgnoreCase(nombre)
                && nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(
                        nombre, procesoId, empresaId)) {
            throw new ReglaNegocioException("Ya existe un nodo con el nombre \"" + nombre + "\" en este proceso.");
        }
        actividad.setNombre(nombre);
        actividad.setDescripcion(descripcion);
        if (tipoActividad != null) {
            actividad.setTipoActividad(tipoActividad);
        }
        actividad.setPosicionX(posX);
        actividad.setPosicionY(posY);
        actividad = (Actividad) nodoFlujoRepository.save(actividad);
        auditoriaModeladoService.registrar(actividad.getLane().getPool().getProceso(),
                "Actividad editada: " + actividad.getNombre() + ".");
        return actividad;
    }

    @Transactional
    public void eliminar(Long empresaId, Long actividadId) {
        Actividad actividad = obtener(empresaId, actividadId);
        var proceso = actividad.getLane().getPool().getProceso();
        String nombre = actividad.getNombre();
        arcoRepository.findAllByOrigenIdAndEmpresaId(actividadId, empresaId).forEach(arco -> {
            arco.setActivo(false);
            arcoRepository.save(arco);
        });
        arcoRepository.findAllByDestinoIdAndEmpresaId(actividadId, empresaId).forEach(arco -> {
            arco.setActivo(false);
            arcoRepository.save(arco);
        });
        actividad.setActivo(false);
        nodoFlujoRepository.save(actividad);
        auditoriaModeladoService.registrar(proceso, "Actividad eliminada (baja logica): " + nombre + ".");
    }

    public Actividad obtener(Long empresaId, Long actividadId) {
        return nodoFlujoRepository.findByIdAndEmpresaId(actividadId, empresaId)
                .filter(nodo -> nodo.isActivo())
                .filter(Actividad.class::isInstance)
                .map(Actividad.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada."));
    }

    public List<Actividad> listarPorLane(Long empresaId, Long laneId) {
        if (!laneRepository.existsByIdAndEmpresaId(laneId, empresaId)) {
            throw new RecursoNoEncontradoException("Lane no encontrada.");
        }
        return nodoFlujoRepository.findAllByLaneIdAndEmpresaId(laneId, empresaId).stream()
                .filter(nodo -> nodo.isActivo())
                .filter(Actividad.class::isInstance)
                .map(Actividad.class::cast)
                .toList();
    }

}
