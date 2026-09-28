package com.facimus.procesos.gestion.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

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

    public ProcesoDiagrama obtener(Long empresaId, Long procesoId) {
        Proceso proceso = procesoRepository.findByIdAndEmpresaIdAndActivoTrue(procesoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado."));

        List<Pool> pools = poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaId);
        List<Lane> lanes = new ArrayList<>();
        List<Actividad> actividades = new ArrayList<>();
        List<Gateway> gateways = new ArrayList<>();
        List<EventoMensaje> eventos = new ArrayList<>();
        List<Arco> arcos = new ArrayList<>();

        pools.forEach(pool -> {
            lanes.addAll(laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(pool.getId(), empresaId));
            arcos.addAll(arcoRepository.findAllByPoolIdAndEmpresaId(pool.getId(), empresaId).stream()
                    .filter(Arco::isActivo)
                    .toList());
        });

        lanes.forEach(lane -> nodoFlujoRepository.findAllByLaneIdAndEmpresaId(lane.getId(), empresaId).stream()
                .filter(nodo -> nodo.isActivo())
                .forEach(nodo -> {
                    if (nodo instanceof Actividad actividad) {
                        actividades.add(actividad);
                    } else if (nodo instanceof Gateway gateway) {
                        gateways.add(gateway);
                    } else if (nodo instanceof EventoMensaje evento) {
                        eventos.add(evento);
                    }
                }));

        List<Mensaje> mensajes = mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(procesoId, empresaId);

        return new ProcesoDiagrama(proceso, pools, lanes, actividades, gateways, eventos, arcos, mensajes);
    }
}
