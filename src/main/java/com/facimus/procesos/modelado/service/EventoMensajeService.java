package com.facimus.procesos.modelado.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoMensajeService {

    private final NodoFlujoRepository nodoFlujoRepository;
    private final LaneRepository laneRepository;
    private final ArcoRepository arcoRepository;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public EventoMensaje crear(Long empresaId, Long laneId, String nombre, TipoEventoMensaje tipoEvento,
            String contenido, String claveCorrelacion, int posX, int posY, boolean origenExterno) {
        Lane lane = laneRepository.findByIdAndEmpresaId(laneId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada."));
        if (lane.getPool().isCajaNegra()) {
            throw new ReglaNegocioException("Un pool de caja negra no puede contener eventos internos.");
        }
        validarNombreUnico(empresaId, lane.getPool().getProceso().getId(), nombre, null);

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
        evento = (EventoMensaje) nodoFlujoRepository.save(evento);
        auditoriaModeladoService.registrar(lane.getPool().getProceso(),
                "Evento de mensaje creado: " + evento.getNombre() + " (" + evento.getTipoEvento() + ").");
        return evento;
    }

    @Transactional
    public EventoMensaje editar(Long empresaId, Long eventoId, String nombre, TipoEventoMensaje tipoEvento,
            String contenido, String claveCorrelacion, int posX, int posY, boolean origenExterno) {
        EventoMensaje evento = obtener(empresaId, eventoId);
        validarNombreUnico(empresaId, evento.getLane().getPool().getProceso().getId(), nombre, eventoId);
        if (tipoEvento == TipoEventoMensaje.CATCH_INICIO
                && !arcoRepository.findAllByDestinoIdAndEmpresaId(eventoId, empresaId).isEmpty()) {
            throw new ReglaNegocioException("Un Message Catch de inicio no puede tener arcos entrantes.");
        }
        evento.setNombre(nombre);
        evento.setTipoEvento(tipoEvento);
        evento.setContenido(contenido);
        evento.setClaveCorrelacion(claveCorrelacion);
        evento.setPosicionX(posX);
        evento.setPosicionY(posY);
        evento.setOrigenExterno(origenExterno);
        evento = (EventoMensaje) nodoFlujoRepository.save(evento);
        auditoriaModeladoService.registrar(evento.getLane().getPool().getProceso(),
                "Evento de mensaje editado: " + evento.getNombre() + ".");
        return evento;
    }

    @Transactional
    public void eliminar(Long empresaId, Long eventoId) {
        EventoMensaje evento = obtener(empresaId, eventoId);
        var proceso = evento.getLane().getPool().getProceso();
        String nombre = evento.getNombre();
        arcoRepository.deleteAll(arcoRepository.findAllByOrigenIdAndEmpresaId(eventoId, empresaId));
        arcoRepository.deleteAll(arcoRepository.findAllByDestinoIdAndEmpresaId(eventoId, empresaId));
        nodoFlujoRepository.delete(evento);
        auditoriaModeladoService.registrar(proceso, "Evento de mensaje eliminado: " + nombre + ".");
    }

    public EventoMensaje obtener(Long empresaId, Long eventoId) {
        return nodoFlujoRepository.findByIdAndEmpresaId(eventoId, empresaId)
                .filter(EventoMensaje.class::isInstance)
                .map(EventoMensaje.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento de mensaje no encontrado."));
    }

    public List<EventoMensaje> listarPorLane(Long empresaId, Long laneId) {
        if (!laneRepository.existsByIdAndEmpresaId(laneId, empresaId)) {
            throw new RecursoNoEncontradoException("Lane no encontrada.");
        }
        return nodoFlujoRepository.findAllByLaneIdAndEmpresaId(laneId, empresaId).stream()
                .filter(EventoMensaje.class::isInstance)
                .map(EventoMensaje.class::cast)
                .toList();
    }

    private void validarNombreUnico(Long empresaId, Long procesoId, String nombre, Long nodoActualId) {
        boolean existe = nodoFlujoRepository.findAllByEmpresaId(empresaId).stream()
                .filter(nodo -> nodo.getLane().getPool().getProceso().getId().equals(procesoId))
                .anyMatch(nodo -> nodo.getNombre().equalsIgnoreCase(nombre)
                        && (nodoActualId == null || !nodo.getId().equals(nodoActualId)));
        if (existe) {
            throw new ReglaNegocioException("Ya existe un nodo con el nombre "" + nombre + "" en este proceso.");
        }
    }
}
