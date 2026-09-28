package com.facimus.procesos.gestion.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.controller.dto.ProcesoDiagramaResponse;
import com.facimus.procesos.gestion.controller.dto.ProcesoResponse;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.modelado.controller.dto.ActividadResponse;
import com.facimus.procesos.modelado.controller.dto.ArcoResponse;
import com.facimus.procesos.modelado.controller.dto.EventoMensajeResponse;
import com.facimus.procesos.modelado.controller.dto.GatewayResponse;
import com.facimus.procesos.modelado.controller.dto.LaneResponse;
import com.facimus.procesos.modelado.controller.dto.MensajeResponse;
import com.facimus.procesos.modelado.controller.dto.PoolResponse;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

import lombok.RequiredArgsConstructor;

/** Arma una vista completa del diagrama BPMN de un proceso. */
@Service
@RequiredArgsConstructor
public class ProcesoDiagramaService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final LaneRepository laneRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final ArcoRepository arcoRepository;
    private final MensajeRepository mensajeRepository;

    public ProcesoDiagramaResponse obtener(Long empresaId, Long procesoId) {
        Proceso proceso = procesoRepository.findByIdAndEmpresaIdAndActivoTrue(procesoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado."));

        List<Pool> pools = poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaId);
        List<Lane> lanes = new ArrayList<>();
        pools.forEach(pool -> lanes.addAll(
                laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(pool.getId(), empresaId)));

        List<ActividadResponse> actividades = new ArrayList<>();
        List<GatewayResponse> gateways = new ArrayList<>();
        List<EventoMensajeResponse> eventos = new ArrayList<>();
        lanes.forEach(lane -> nodoFlujoRepository.findAllByLaneIdAndEmpresaId(lane.getId(), empresaId)
                .filter(nodo -> nodo.isActivo())\n                .forEach(nodo -> {
                    if (nodo instanceof Actividad actividad) {
                        actividades.add(ActividadResponse.of(actividad));
                    } else if (nodo instanceof Gateway gateway) {
                        gateways.add(GatewayResponse.of(gateway));
                    } else if (nodo instanceof EventoMensaje evento) {
                        eventos.add(EventoMensajeResponse.of(evento));
                    }
                }));

        List<ArcoResponse> arcos = new ArrayList<>();
        pools.forEach(pool -> arcoRepository.findAllByPoolIdAndEmpresaId(pool.getId(), empresaId)
                .stream().filter(arco -> arco.isActivo())\n                .forEach(arco -> arcos.add(ArcoResponse.of(arco))));

        return new ProcesoDiagramaResponse(
                ProcesoResponse.of(proceso),
                pools.stream().map(PoolResponse::of).toList(),
                lanes.stream().map(LaneResponse::of).toList(),
                actividades,
                gateways,
                eventos,
                arcos,
                mensajeRepository.findAllByProcesoIdAndEmpresaId(procesoId, empresaId).stream()
                        .map(MensajeResponse::of).toList());
    }
}
