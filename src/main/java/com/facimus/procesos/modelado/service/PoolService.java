package com.facimus.procesos.modelado.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PoolService {

    private final PoolRepository poolRepository;
    private final ProcesoRepository procesoRepository;
    private final LaneRepository laneRepository;
    private final NodoFlujoRepository nodoFlujoRepository;
    private final AuditoriaModeladoService auditoriaModeladoService;
    private final PermisoPoolService permisoPoolService;

    @Transactional
    public Pool crear(Long empresaId, Long procesoId, String nombre, TipoParticipante tipoParticipante,
            boolean cajaNegra) {
        Proceso proceso = procesoRepository.findByIdAndEmpresaId(procesoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado."));
        int orden = poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaId).size();

        Pool pool = new Pool();
        pool.setEmpresa(proceso.getEmpresa());
        pool.setProceso(proceso);
        pool.setNombre(nombre);
        pool.setTipoParticipante(tipoParticipante);
        pool.setCajaNegra(cajaNegra);
        pool.setOrden(orden);
        pool.setRolesEdicion(new LinkedHashSet<>(Set.of(RolAcceso.ADMINISTRADOR, RolAcceso.EDITOR)));
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
        permisoPoolService.validarEdicion(pool);
        if (cajaNegra && !pool.isCajaNegra()
                && !laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId).isEmpty()) {
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
    public Pool configurarPermisos(Long empresaId, Long poolId, Set<RolAcceso> rolesEdicion) {
        Pool pool = obtener(empresaId, poolId);
        permisoPoolService.validarConfiguracion();
        if (rolesEdicion.contains(RolAcceso.LECTURA) && rolesEdicion.size() == 1) {
            throw new ReglaNegocioException("El pool debe conservar al menos un rol con capacidad de edicion.");
        }
        pool.setRolesEdicion(new LinkedHashSet<>(rolesEdicion));
        pool = poolRepository.save(pool);
        auditoriaModeladoService.registrar(pool.getProceso(), "Permisos de edicion actualizados para el pool "
                + pool.getNombre() + ".");
        return pool;
    }

    @Transactional
    public void eliminar(Long empresaId, Long poolId) {
        Pool pool = obtener(empresaId, poolId);
        permisoPoolService.validarEdicion(pool);
        boolean tieneNodos = laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId)
                .stream()
                .anyMatch(lane -> !nodoFlujoRepository.findAllByLaneIdAndEmpresaId(lane.getId(), empresaId).isEmpty());
        if (tieneNodos) {
            throw new ReglaNegocioException("El pool "" + pool.getNombre()
                    + "" tiene lanes con elementos; no se puede eliminar.");
        }
        var proceso = pool.getProceso();
        String nombre = pool.getNombre();
        laneRepository.deleteAll(laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(poolId, empresaId));
        poolRepository.delete(pool);
        auditoriaModeladoService.registrar(proceso, "Pool eliminado: " + nombre + ".");
    }

    public List<Pool> listarPorProceso(Long empresaId, Long procesoId) {
        if (!procesoRepository.existsByIdAndEmpresaId(procesoId, empresaId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado.");
        }
        return poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(procesoId, empresaId);
    }

    public Pool obtener(Long empresaId, Long poolId) {
        return poolRepository.findByIdAndEmpresaId(poolId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado."));
    }
}
