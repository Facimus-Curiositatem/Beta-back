package com.facimus.procesos.modelado.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.NodoFlujo;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NodoFlujoService {

    private final NodoFlujoRepository nodoFlujoRepository;

    public NodoFlujo obtener(Long empresaId, Long nodoId) {
        return nodoFlujoRepository.findByIdAndEmpresaId(nodoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nodo no encontrado."));
    }

    public Optional<NodoFlujo> buscar(Long empresaId, Long nodoId) {
        return nodoFlujoRepository.findByIdAndEmpresaId(nodoId, empresaId);
    }

    public List<NodoFlujo> listarActivosPorProceso(Long empresaId, Long procesoId) {
        return nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(procesoId, empresaId).stream()
                .filter(NodoFlujo::isActivo)
                .toList();
    }

    public List<NodoFlujo> listarPorLane(Long empresaId, Long laneId) {
        return nodoFlujoRepository.findAllByLaneIdAndEmpresaId(laneId, empresaId);
    }

    public boolean tieneNodosActivos(Long empresaId, Long laneId) {
        return nodoFlujoRepository.findAllByLaneIdAndEmpresaId(laneId, empresaId).stream()
                .anyMatch(NodoFlujo::isActivo);
    }
}
