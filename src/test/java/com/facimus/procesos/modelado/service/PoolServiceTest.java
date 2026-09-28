package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
class PoolServiceTest {

    @Mock
    private PoolRepository poolRepository;
    @Mock
    private ProcesoRepository procesoRepository;
    @Mock
    private LaneRepository laneRepository;
    @Mock
    private NodoFlujoRepository nodoFlujoRepository;
    @Mock
    private AuditoriaModeladoService auditoriaModeladoService;

    @InjectMocks
    private PoolService poolService;

    private Empresa empresa;
    private Proceso proceso;
    private Pool pool;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);

        proceso = new Proceso();
        proceso.setId(10L);
        proceso.setEmpresa(empresa);

        pool = new Pool();
        pool.setId(100L);
        pool.setNombre("Pool Principal");
        pool.setProceso(proceso);
        pool.setEmpresa(empresa);
        pool.setTipoParticipante(TipoParticipante.EMPRESA);
    }

    @Test
    @DisplayName("Crear pool asigna orden correcto")
    void crear_exitoso() {
        when(procesoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(proceso));
        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L))
                .thenReturn(List.of(pool));
        when(poolRepository.save(any(Pool.class))).thenAnswer(inv -> inv.getArgument(0));

        Pool resultado = poolService.crear(1L, 10L, "Nuevo Pool", TipoParticipante.CLIENTE, false);

        assertEquals("Nuevo Pool", resultado.getNombre());
        assertEquals(1, resultado.getOrden());
    }

    @Test
    @DisplayName("Crear pool con proceso inexistente lanza excepcion")
    void crear_proceso_inexistente() {
        when(procesoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> poolService.crear(1L, 10L, "Pool", TipoParticipante.EMPRESA, false));
    }

    @Test
    @DisplayName("Editar pool actualiza nombre y tipo")
    void editar_exitoso() {
        when(poolRepository.findByIdAndEmpresaId(100L, 1L)).thenReturn(Optional.of(pool));
        when(poolRepository.save(any(Pool.class))).thenAnswer(inv -> inv.getArgument(0));

        Pool resultado = poolService.editar(1L, 100L, "Pool Editado", TipoParticipante.PROVEEDOR);

        assertEquals("Pool Editado", resultado.getNombre());
        assertEquals(TipoParticipante.PROVEEDOR, resultado.getTipoParticipante());
    }

    @Test
    @DisplayName("Eliminar pool vacio elimina lanes y pool")
    void eliminar_pool_vacio() {
        Lane lane = new Lane();
        lane.setId(1000L);
        when(poolRepository.findByIdAndEmpresaId(100L, 1L)).thenReturn(Optional.of(pool));
        when(laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(100L, 1L))
                .thenReturn(List.of(lane));
        when(nodoFlujoRepository.findAllByLaneIdAndEmpresaId(1000L, 1L))
                .thenReturn(Collections.emptyList());

        poolService.eliminar(1L, 100L);

        verify(laneRepository).deleteAll(anyList());
        verify(poolRepository).delete(pool);
    }

    @Test
    @DisplayName("Eliminar pool con actividades lanza excepcion")
    void eliminar_pool_con_actividades() {
        Lane lane = new Lane();
        lane.setId(1000L);
        when(poolRepository.findByIdAndEmpresaId(100L, 1L)).thenReturn(Optional.of(pool));
        when(laneRepository.findAllByPoolIdAndEmpresaIdOrderByOrdenAsc(100L, 1L))
                .thenReturn(List.of(lane));
        when(nodoFlujoRepository.findAllByLaneIdAndEmpresaId(1000L, 1L))
                .thenReturn(List.of(new Actividad()));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> poolService.eliminar(1L, 100L));

        assertTrue(ex.getMessage().contains("actividades"));
        verify(poolRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Listar pools por proceso inexistente lanza excepcion")
    void listar_proceso_inexistente() {
        when(procesoRepository.existsByIdAndEmpresaId(10L, 1L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class,
                () -> poolService.listarPorProceso(1L, 10L));
    }

    @Test
    @DisplayName("Listar pools por proceso existente retorna lista")
    void listar_exitoso() {
        when(procesoRepository.existsByIdAndEmpresaId(10L, 1L)).thenReturn(true);
        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L))
                .thenReturn(List.of(pool));

        List<Pool> resultado = poolService.listarPorProceso(1L, 10L);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Obtener pool inexistente lanza excepcion")
    void obtener_inexistente() {
        when(poolRepository.findByIdAndEmpresaId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> poolService.obtener(1L, 999L));
    }
}
