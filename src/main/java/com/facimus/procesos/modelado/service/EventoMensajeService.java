package com.facimus.procesos.modelado.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoMensajeService {

    private final NodoFlujoService nodoFlujoService;
    private final LaneService laneService;
    private final ArcoService arcoService;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public EventoMensaje crear(Long empresaId, Long laneId, String nombre, TipoEventoMensaje tipoEvento,
            String contenido, String claveCorrelacion, int posX, int posY, boolean origenExterno) {
        Lane lane = laneService.obtener(empresaId, laneId);
        validarProcesoActivo(lane.getPool().getProceso());
        if (lane.getPool().isCajaNegra()) {
            throw new ReglaNegocioException("Un pool de caja negra no puede contener eventos internos.");
        }
        Long procesoId = lane.getPool().getProceso().getId();
        if (nombreOcupado(empresaId, procesoId, nombre, tipoEvento, null)) {
            throw new ReglaNegocioException("Ya existe un nodo con el nombre \"" + nombre + "\" en este proceso.");
        }

        EventoMensaje evento = new EventoMensaje();
        evento.setEmpresa(lane.getEmpresa());
        evento.setLane(lane);
        evento.setNombre(nombre);
        evento.setTipoEvento(tipoEvento);
        evento.setContenido(contenido);
        evento.setClaveCorrelacion(claveCorrelacion);
        evento.setPosicionX(posX);
        evento.setPosicionY(posY);
        evento.setOrigenExterno(origenExterno);
        evento = (EventoMensaje) nodoFlujoService.guardar(evento);
        auditoriaModeladoService.registrar(lane.getPool().getProceso(),
                "Evento de mensaje creado: " + evento.getNombre() + " (" + evento.getTipoEvento() + ").");
        return evento;
    }

    @Transactional
    public EventoMensaje editar(Long empresaId, Long eventoId, String nombre, TipoEventoMensaje tipoEvento,
            String contenido, String claveCorrelacion, int posX, int posY, boolean origenExterno) {
        EventoMensaje evento = obtener(empresaId, eventoId);
        validarProcesoActivo(evento.getLane().getPool().getProceso());
        Long procesoId = evento.getLane().getPool().getProceso().getId();
        if (!evento.getNombre().equalsIgnoreCase(nombre)
                && nombreOcupado(empresaId, procesoId, nombre, tipoEvento, eventoId)) {
            throw new ReglaNegocioException("Ya existe un nodo con el nombre \"" + nombre + "\" en este proceso.");
        }
        if (tipoEvento == TipoEventoMensaje.CATCH_INICIO
                && arcoService.listarPorDestino(empresaId, eventoId).stream()
                        .anyMatch(arco -> arco.isActivo())) {
            throw new ReglaNegocioException("Un Message Catch de inicio no puede tener arcos entrantes.");
        }
        evento.setNombre(nombre);
        evento.setTipoEvento(tipoEvento);
        evento.setContenido(contenido);
        evento.setClaveCorrelacion(claveCorrelacion);
        evento.setPosicionX(posX);
        evento.setPosicionY(posY);
        evento.setOrigenExterno(origenExterno);
        evento = (EventoMensaje) nodoFlujoService.guardar(evento);
        auditoriaModeladoService.registrar(evento.getLane().getPool().getProceso(),
                "Evento de mensaje editado: " + evento.getNombre() + ".");
        return evento;
    }

    /**
     * Un Throw y su Catch deben llamarse igual (correlacion). El nombre solo esta ocupado si lo usa
     * un nodo que no es evento de mensaje, o un evento de mensaje del mismo lado (Throw/Throw, Catch/Catch).
     */
    private boolean nombreOcupado(Long empresaId, Long procesoId, String nombre,
            TipoEventoMensaje tipoEvento, Long excluirId) {
        boolean esThrow = tipoEvento == TipoEventoMensaje.THROW;
        return nodoFlujoService.listarActivosPorProceso(empresaId, procesoId).stream()
                .filter(nodo -> excluirId == null || !nodo.getId().equals(excluirId))
                .filter(nodo -> nodo.getNombre() != null && nodo.getNombre().equalsIgnoreCase(nombre))
                .anyMatch(nodo -> !(nodo instanceof EventoMensaje otro)
                        || (otro.getTipoEvento() == TipoEventoMensaje.THROW) == esThrow);
    }

    @Transactional
    public void eliminar(Long empresaId, Long eventoId) {
        EventoMensaje evento = obtener(empresaId, eventoId);
        validarProcesoActivo(evento.getLane().getPool().getProceso());
        var proceso = evento.getLane().getPool().getProceso();
        String nombre = evento.getNombre();
        arcoService.desactivarPorNodo(empresaId, eventoId);
        evento.setActivo(false);
        nodoFlujoService.guardar(evento);
        auditoriaModeladoService.registrar(proceso, "Evento de mensaje eliminado (baja logica): " + nombre + ".");
    }

    @Transactional(readOnly = true)
    public EventoMensaje obtener(Long empresaId, Long eventoId) {
        return nodoFlujoService.buscar(empresaId, eventoId)
                .filter(nodo -> nodo.isActivo())
                .filter(EventoMensaje.class::isInstance)
                .map(EventoMensaje.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje no encontrado."));
    }

    @Transactional(readOnly = true)
    public List<EventoMensaje> listarPorLane(Long empresaId, Long laneId) {
        laneService.obtener(empresaId, laneId);
        return nodoFlujoService.listarPorLane(empresaId, laneId).stream()
                .filter(nodo -> nodo.isActivo())
                .filter(EventoMensaje.class::isInstance)
                .map(EventoMensaje.class::cast)
                .toList();
    }

    private void validarProcesoActivo(Proceso proceso) {
        if (!proceso.isActivo()) {
            throw new ReglaNegocioException("No se permite modelar en un proceso inactivo.");
        }
    }
}
