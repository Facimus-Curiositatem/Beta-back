package com.facimus.procesos.modelado.service;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.gestion.service.RolProcesoService;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.repository.LaneRepository;

@Service
public class LaneService {

    private final LaneRepository laneRepository;
    private final PoolService poolService;
    private final RolProcesoService rolProcesoService;
    private final NodoFlujoService nodoFlujoService;
    private final AuditoriaModeladoService auditoriaModeladoService;

    public LaneService(LaneRepository laneRepository, @Lazy PoolService poolService,
            RolProcesoService rolProcesoService, NodoFlujoService nodoFlujoService,
            AuditoriaModeladoService auditoriaModeladoService) {
        this.laneRepository = laneRepository;
        this.poolService = poolService;
        this.rolProcesoService = rolProcesoService;
        this.nodoFlujoService = nodoFlujoService;
        this.auditoriaModeladoService = auditoriaModeladoService;
    }

    @Transactional
    public Lane crear(Long empresaId, Long poolId, String nombre, Long rolProcesoId) {
        Pool pool = poolService.obtener(empresaId, poolId);
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool de caja negra no puede contener lanes.");
        }
        RolProceso rolProceso = rolProcesoService.obtener(empresaId, rolProcesoId);
        int orden = laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId).size();

        Lane lane = new Lane();
        lane.setEmpresa(pool.getEmpresa());
        lane.setPool(pool);
        lane.setNombre(nombre);
        lane.setRolProceso(rolProceso);
        lane.setOrden(orden);
        lane = laneRepository.save(lane);
        auditoriaModeladoService.registrar(pool.getProceso(), "Lane creada: " + lane.getNombre() + ".");
        return lane;
    }

    @Transactional
    public Lane editar(Long empresaId, Long laneId, String nombre, Long rolProcesoId) {
        Lane lane = obtener(empresaId, laneId);
        RolProceso rolProceso = rolProcesoService.obtener(empresaId, rolProcesoId);
        lane.setNombre(nombre);
        lane.setRolProceso(rolProceso);
        lane = laneRepository.save(lane);
        auditoriaModeladoService.registrar(lane.getPool().getProceso(), "Lane editada: " + lane.getNombre() + ".");
        return lane;
    }

    @Transactional
    public void eliminar(Long empresaId, Long laneId) {
        Lane lane = obtener(empresaId, laneId);
        if (nodoFlujoService.tieneNodosActivos(empresaId, laneId)) {
            throw new ReglaNegocioException("La lane \"" + lane.getNombre()
                    + "\" contiene elementos activos; primero deben reasignarse.");
        }
        var proceso = lane.getPool().getProceso();
        String nombre = lane.getNombre();
        laneRepository.delete(lane);
        auditoriaModeladoService.registrar(proceso, "Lane eliminada: " + nombre + ".");
    }

    @Transactional
    public List<Lane> reordenar(Long empresaId, Long poolId, List<Long> laneIds) {
        Pool pool = poolService.obtener(empresaId, poolId);
        List<Lane> lanes = laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId);
        java.util.Set<Long> idsUnicos = new java.util.LinkedHashSet<>(laneIds);
        if (idsUnicos.size() != laneIds.size()) {
            throw new ReglaNegocioException("La lista de IDs contiene duplicados.");
        }
        if (lanes.size() != laneIds.size()) {
            throw new ReglaNegocioException("La lista de IDs debe contener exactamente todas las lanes del pool.");
        }
        java.util.Map<Long, Lane> laneMap = new java.util.HashMap<>();
        lanes.forEach(l -> laneMap.put(l.getId(), l));
        for (Long id : laneIds) {
            if (!laneMap.containsKey(id)) {
                throw new ReglaNegocioException("El lane con ID " + id + " no pertenece a este pool.");
            }
        }
        for (int i = 0; i < laneIds.size(); i++) {
            Lane lane = laneMap.get(laneIds.get(i));
            lane.setOrden(i);
            laneRepository.save(lane);
        }
        auditoriaModeladoService.registrar(pool.getProceso(), "Lanes reordenadas en el pool " + pool.getNombre() + ".");
        return laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId);
    }

    public List<Lane> listarPorPool(Long empresaId, Long poolId) {
        poolService.obtener(empresaId, poolId);
        return laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId);
    }

    public Lane obtener(Long empresaId, Long laneId) {
        return laneRepository.findByIdAndEmpresaId(laneId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada."));
    }

    public List<Lane> listarPorProceso(Long empresaId, Long procesoId) {
        return laneRepository.findAllByPool_ProcesoIdAndEmpresaId(procesoId, empresaId);
    }

    public List<Lane> listarPorRolProceso(Long empresaId, Long rolProcesoId) {
        return laneRepository.findAllByRolProcesoIdAndEmpresaId(rolProcesoId, empresaId);
    }

    public long contarPorRolProceso(Long empresaId, Long rolProcesoId) {
        return laneRepository.countByRolProcesoIdAndEmpresaId(rolProcesoId, empresaId);
    }

    @Transactional
    public void eliminarPorPool(Long empresaId, Long poolId) {
        laneRepository.deleteAll(laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId));
    }

    public boolean existe(Long empresaId, Long laneId) {
        return laneRepository.existsByIdAndEmpresaId(laneId, empresaId);
    }
}
