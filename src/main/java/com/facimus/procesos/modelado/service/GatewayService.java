package com.facimus.procesos.modelado.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.TipoGateway;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GatewayService {

    private final NodoFlujoRepository nodoFlujoRepository;
    private final LaneRepository laneRepository;
    private final ArcoRepository arcoRepository;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public Gateway crear(Long empresaId, Long laneId, String nombre, TipoGateway tipoGateway, int posX, int posY) {
        Lane lane = laneRepository.findByIdAndEmpresaId(laneId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada."));
        if (lane.getPool().isCajaNegra()) {
            throw new ReglaNegocioException("Un pool de caja negra no puede contener gateways.");
        }
        Long procesoId = lane.getPool().getProceso().getId();
        if (nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(nombre, procesoId, empresaId)) {
            throw new ReglaNegocioException("Ya existe un nodo con el nombre "" + nombre + "" en este proceso.");
        }

        Gateway gateway = new Gateway();
        gateway.setEmpresa(lane.getEmpresa());
        gateway.setLane(lane);
        gateway.setNombre(nombre);
        gateway.setTipoGateway(tipoGateway);
        gateway.setPosicionX(posX);
        gateway.setPosicionY(posY);
        gateway = (Gateway) nodoFlujoRepository.save(gateway);
        auditoriaModeladoService.registrar(lane.getPool().getProceso(),
                "Gateway creado: " + gateway.getNombre() + " (" + gateway.getTipoGateway() + ").");
        return gateway;
    }

    @Transactional
    public Gateway editar(Long empresaId, Long gatewayId, String nombre, TipoGateway tipoGateway, int posX, int posY) {
        Gateway gateway = obtener(empresaId, gatewayId);
        Long procesoId = gateway.getLane().getPool().getProceso().getId();
        if (!gateway.getNombre().equalsIgnoreCase(nombre)
                && nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(
                        nombre, procesoId, empresaId)) {
            throw new ReglaNegocioException("Ya existe un nodo con el nombre "" + nombre + "" en este proceso.");
        }
        if (tipoGateway == TipoGateway.PARALELO && gateway.getTipoGateway() != TipoGateway.PARALELO) {
            arcoRepository.findAllByOrigenIdAndEmpresaId(gatewayId, empresaId)
                    .forEach(arco -> {
                        arco.setCondicion(null);
                        arcoRepository.save(arco);
                    });
        }
        if ((tipoGateway == TipoGateway.EXCLUSIVO || tipoGateway == TipoGateway.INCLUSIVO)
                && gateway.getTipoGateway() == TipoGateway.PARALELO) {
            boolean arcosSinCondicion = arcoRepository.findAllByOrigenIdAndEmpresaId(gatewayId, empresaId).stream()
                    .anyMatch(arco -> !org.springframework.util.StringUtils.hasText(arco.getCondicion()));
            if (arcosSinCondicion) {
                throw new ReglaNegocioException(
                        "No se puede cambiar a " + tipoGateway + " porque existen arcos salientes sin condicion.");
            }
        }
        gateway.setNombre(nombre);
        gateway.setTipoGateway(tipoGateway);
        gateway.setPosicionX(posX);
        gateway.setPosicionY(posY);
        gateway = (Gateway) nodoFlujoRepository.save(gateway);
        auditoriaModeladoService.registrar(gateway.getLane().getPool().getProceso(),
                "Gateway editado: " + gateway.getNombre() + ".");
        return gateway;
    }

    @Transactional
    public void eliminar(Long empresaId, Long gatewayId) {
        Gateway gateway = obtener(empresaId, gatewayId);
        var proceso = gateway.getLane().getPool().getProceso();
        String nombre = gateway.getNombre();
        arcoRepository.deleteAll(arcoRepository.findAllByOrigenIdAndEmpresaId(gatewayId, empresaId));
        arcoRepository.deleteAll(arcoRepository.findAllByDestinoIdAndEmpresaId(gatewayId, empresaId));
        nodoFlujoRepository.delete(gateway);
        auditoriaModeladoService.registrar(proceso, "Gateway eliminado: " + nombre + ".");
    }

    public Gateway obtener(Long empresaId, Long gatewayId) {
        return nodoFlujoRepository.findByIdAndEmpresaId(gatewayId, empresaId)
                .filter(Gateway.class::isInstance)
                .map(Gateway.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado."));
    }

    public List<Gateway> listarPorLane(Long empresaId, Long laneId) {
        if (!laneRepository.existsByIdAndEmpresaId(laneId, empresaId)) {
            throw new RecursoNoEncontradoException("Lane no encontrada.");
        }
        return nodoFlujoRepository.findAllByLaneIdAndEmpresaId(laneId, empresaId).stream()
                .filter(Gateway.class::isInstance)
                .map(Gateway.class::cast)
                .toList();
    }
}
