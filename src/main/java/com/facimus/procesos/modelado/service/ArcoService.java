package com.facimus.procesos.modelado.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.NodoFlujo;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;
import com.facimus.procesos.modelado.model.TipoGateway;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArcoService {

    private final ArcoRepository arcoRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final PoolRepository poolRepository;
    private final AuditoriaModeladoService auditoriaModeladoService;

    @Transactional
    public Arco crear(Long empresaId, Long origenId, Long destinoId, String etiqueta, String condicion) {
        if (origenId.equals(destinoId)) {
            throw new ReglaNegocioException("Un arco no puede tener el mismo nodo como origen y destino.");
        }
        NodoFlujo origen = nodoFlujoRepository.findByIdAndEmpresaId(origenId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nodo de origen no encontrado."));
        NodoFlujo destino = nodoFlujoRepository.findByIdAndEmpresaId(destinoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nodo de destino no encontrado."));

        validarConexion(empresaId, origen, destino, condicion, null);

        Arco arco = new Arco();
        arco.setEmpresa(origen.getEmpresa());
        arco.setOrigen(origen);
        arco.setDestino(destino);
        arco.setPool(origen.getLane().getPool());
        arco.setEtiqueta(etiqueta);
        arco.setCondicion(condicion);
        arco = arcoRepository.save(arco);
        auditoriaModeladoService.registrar(origen.getLane().getPool().getProceso(),
                "Arco creado: " + origen.getNombre() + " -> " + destino.getNombre() + ".");
        return arco;
    }

    @Transactional
    public Arco editar(Long empresaId, Long arcoId, String etiqueta, String condicion,
            Long origenId, Long destinoId) {
        Arco arco = obtener(empresaId, arcoId);
        NodoFlujo origen = arco.getOrigen();
        NodoFlujo destino = arco.getDestino();

        Long nuevoOrigenId = origenId != null ? origenId : origen.getId();
        Long nuevoDestinoId = destinoId != null ? destinoId : destino.getId();
        if (nuevoOrigenId.equals(nuevoDestinoId)) {
            throw new ReglaNegocioException("Un arco no puede tener el mismo nodo como origen y destino.");
        }
        if (origenId != null) {
            origen = nodoFlujoRepository.findByIdAndEmpresaId(origenId, empresaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Nodo de origen no encontrado."));
        }
        if (destinoId != null) {
            destino = nodoFlujoRepository.findByIdAndEmpresaId(destinoId, empresaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Nodo de destino no encontrado."));
        }

        validarConexion(empresaId, origen, destino, condicion, arcoId);
        arco.setOrigen(origen);
        arco.setDestino(destino);
        arco.setPool(origen.getLane().getPool());
        arco.setEtiqueta(etiqueta);
        arco.setCondicion(condicion);
        arco = arcoRepository.save(arco);
        auditoriaModeladoService.registrar(origen.getLane().getPool().getProceso(),
                "Arco editado: " + origen.getNombre() + " -> " + destino.getNombre() + ".");
        return arco;
    }

    @Transactional
    public void eliminar(Long empresaId, Long arcoId) {
        Arco arco = obtener(empresaId, arcoId);
        var proceso = arco.getPool().getProceso();
        String descripcion = arco.getOrigen().getNombre() + " -> " + arco.getDestino().getNombre();
        arcoRepository.delete(arco);
        auditoriaModeladoService.registrar(proceso, "Arco eliminado: " + descripcion + ".");
    }

    public Arco obtener(Long empresaId, Long arcoId) {
        return arcoRepository.findByIdAndEmpresaId(arcoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Arco no encontrado."));
    }

    public List<Arco> listarPorPool(Long empresaId, Long poolId) {
        if (!poolRepository.existsByIdAndEmpresaId(poolId, empresaId)) {
            throw new RecursoNoEncontradoException("Pool no encontrado.");
        }
        return arcoRepository.findAllByPoolIdAndEmpresaId(poolId, empresaId);
    }

    private void validarConexion(Long empresaId, NodoFlujo origen, NodoFlujo destino, String condicion,
            Long arcoActualId) {
        Pool poolOrigen = origen.getLane().getPool();
        Pool poolDestino = destino.getLane().getPool();
        if (!poolOrigen.getId().equals(poolDestino.getId())) {
            throw new ReglaNegocioException("El origen y el destino de un arco deben pertenecer al mismo pool.");
        }
        boolean duplicado = arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(origen.getId(), destino.getId(),
                empresaId);
        if (duplicado && (arcoActualId == null
                || !esMismoArco(arcoActualId, empresaId, origen.getId(), destino.getId()))) {
            throw new ReglaNegocioException("Ya existe un arco entre estos dos nodos.");
        }
        if (origen instanceof Gateway gatewayOrigen
                && (gatewayOrigen.getTipoGateway() == TipoGateway.EXCLUSIVO
                        || gatewayOrigen.getTipoGateway() == TipoGateway.INCLUSIVO)
                && !StringUtils.hasText(condicion)) {
            throw new ReglaNegocioException("Un arco desde un gateway exclusivo o inclusivo requiere condicion.");
        }
        if (destino instanceof EventoMensaje evento
                && evento.getTipoEvento() == TipoEventoMensaje.CATCH_INICIO) {
            throw new ReglaNegocioException("Un Message Catch de inicio no puede tener arcos entrantes.");
        }
    }

    private boolean esMismoArco(Long arcoId, Long empresaId, Long origenId, Long destinoId) {
        return arcoRepository.findByIdAndEmpresaId(arcoId, empresaId)
                .map(arco -> arco.getOrigen().getId().equals(origenId)
                        && arco.getDestino().getId().equals(destinoId))
                .orElse(false);
    }
}
