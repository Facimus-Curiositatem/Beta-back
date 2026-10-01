package com.facimus.procesos.gestion.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.gestion.service.dto.ProcesoDiagrama;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcesoDiagramaService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final LaneRepository laneRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final ArcoRepository arcoRepository;
    private final MensajeRepository mensajeRepository;

    @Transactional(readOnly = true)
    public ProcesoDiagrama obtener(Long empresaId, Long procesoId) {
        Proceso proceso = procesoRepository.findByIdAndEmpresaIdAndActivoTrue(procesoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado."));

        List<Pool> pools = poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaId);
        List<Lane> lanes = laneRepository.findAllByPool_ProcesoIdAndEmpresaId(procesoId, empresaId);
        var nodos = nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(procesoId, empresaId).stream()
                .filter(nodo -> nodo.isActivo())
                .toList();
        List<Actividad> actividades = nodos.stream()
                .filter(Actividad.class::isInstance).map(Actividad.class::cast).toList();
        List<Gateway> gateways = nodos.stream()
                .filter(Gateway.class::isInstance).map(Gateway.class::cast).toList();
        List<EventoMensaje> eventos = nodos.stream()
                .filter(EventoMensaje.class::isInstance).map(EventoMensaje.class::cast).toList();
        List<Arco> arcos = arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(procesoId, empresaId).stream()
                .filter(Arco::isActivo).toList();
        List<Mensaje> mensajes = mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(procesoId, empresaId);

        return new ProcesoDiagrama(proceso, pools, lanes, actividades, gateways, eventos, arcos, mensajes);
    }
}
