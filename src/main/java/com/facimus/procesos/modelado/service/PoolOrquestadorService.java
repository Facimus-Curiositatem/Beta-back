package com.facimus.procesos.modelado.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.modelado.model.Pool;

import lombok.RequiredArgsConstructor;

/**
 * Orquestador transaccional del caso de uso HU-21 (eliminar un pool junto con sus lanes). Depende
 * de PoolService y LaneService, ninguno de los cuales depende de este orquestador, asi que no
 * reintroduce el ciclo PoolService<->LaneService que el refactor elimino.
 */
@Service
@RequiredArgsConstructor
public class PoolOrquestadorService {

    private final PoolService poolService;
    private final LaneService laneService;

    @Transactional
    public void eliminar(Long empresaId, Long poolId) {
        Pool pool = poolService.verificarEliminable(empresaId, poolId);
        laneService.eliminarPorPool(empresaId, poolId);
        poolService.eliminarRegistro(pool);
    }
}
