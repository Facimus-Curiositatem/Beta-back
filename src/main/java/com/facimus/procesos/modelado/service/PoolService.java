package com.facimus.procesos.modelado.service;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.service.ProcesoService;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.repository.PoolRepository;

@Service
public class PoolService {

    private final PoolRepository poolRepository;
    private final ProcesoService procesoService;
    private final LaneService laneService;
    private final NodoFlujoService nodoFlujoService;
    private final AuditoriaModeladoService auditoriaModeladoService;

    public PoolService(PoolRepository poolRepository, @Lazy ProcesoService procesoService,
            @Lazy LaneService laneService, NodoFlujoService nodoFlujoService,
            AuditoriaModeladoService auditoriaModeladoService) {
        this.poolRepository = poolRepository;
        this.procesoService = procesoService;
        this.laneService = laneService;
        this.nodoFlujoService = nodoFlujoService;
        this.auditoriaModeladoService = auditoriaModeladoService;
    }

    @Transactional
    public Pool crear(Long empresaId, Long procesoId, String nombre, TipoParticipante tipoParticipante,
            boolean cajaNegra) {
        Proceso proceso = procesoService.obtenerPorId(empresaId, procesoId);
        int orden = poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaId).size();

        Pool pool = new Pool();
        pool.setEmpresa(proceso.getEmpresa());
        pool.setProceso(proceso);
        pool.setNombre(nombre);
        pool.setTipoParticipante(tipoParticipante);
        pool.setCajaNegra(cajaNegra);
        pool.setOrden(orden);
        pool = poolRepository.save(pool);
        auditoriaModeladoService.registrar(proceso, "Pool creado: " + pool.getNombre() + ".");
        return pool;
    }

    @Transactional
    public Pool editar(Long empresaId, Long poolId, String nombre, TipoParticipante tipoParticipante) {
        Pool pool = obtener(empresaId, poolId);
        return editar(empresaId, poolId, nombre, tipoParticipante, pool.isCajaNegra());
    }

    @Transactional
    public Pool editar(Long empresaId, Long poolId, String nombre, TipoParticipante tipoParticipante,
            boolean cajaNegra) {
        Pool pool = obtener(empresaId, poolId);
        if (cajaNegra && !pool.isCajaNegra()
                && !laneService.listarPorPool(empresaId, poolId).isEmpty()) {
            throw new ReglaNegocioException("No se puede marcar como caja negra un pool que contiene lanes.");
        }
        pool.setNombre(nombre);
        pool.setTipoParticipante(tipoParticipante);
        pool.setCajaNegra(cajaNegra);
        pool = poolRepository.save(pool);
        auditoriaModeladoService.registrar(pool.getProceso(), "Pool editado: " + pool.getNombre() + ".");
        return pool;
    }

    @Transactional
    public void eliminar(Long empresaId, Long poolId) {
        Pool pool = obtener(empresaId, poolId);
        boolean tieneNodos = laneService.listarPorPool(empresaId, poolId).stream()
                .anyMatch(lane -> nodoFlujoService.tieneNodosActivos(empresaId, lane.getId()));
        if (tieneNodos) {
            throw new ReglaNegocioException("El pool \"" + pool.getNombre()
                    + "\" tiene lanes con actividades o elementos activos; no se puede eliminar.");
        }
        var proceso = pool.getProceso();
        String nombre = pool.getNombre();
        laneService.eliminarPorPool(empresaId, poolId);
        poolRepository.delete(pool);
        auditoriaModeladoService.registrar(proceso, "Pool eliminado: " + nombre + ".");
    }

    public List<Pool> listarPorProceso(Long empresaId, Long procesoId) {
        if (!procesoService.existe(empresaId, procesoId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado.");
        }
        return poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaId);
    }

    public Pool obtener(Long empresaId, Long poolId) {
        return poolRepository.findByIdAndEmpresaId(poolId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado."));
    }

    public boolean existe(Long empresaId, Long poolId) {
        return poolRepository.existsByIdAndEmpresaId(poolId, empresaId);
    }
}
