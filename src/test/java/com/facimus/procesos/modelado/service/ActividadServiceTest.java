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
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
class ActividadServiceTest {

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;
    @Mock
    private LaneRepository laneRepository;
    @Mock
    private ArcoRepository arcoRepository;

    @InjectMocks
    private ActividadService actividadService;

    private Empresa empresa;
    private Proceso proceso;
    private Pool pool1;
    private Pool pool2;
    private Lane lane1;
    private Lane lane2;
    private Lane lane3;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);

        proceso = new Proceso();
        proceso.setId(10L);
        proceso.setEmpresa(empresa);

        pool1 = new Pool();
        pool1.setId(100L);
        pool1.setProceso(proceso);
        pool1.setEmpresa(empresa);

        pool2 = new Pool();
        pool2.setId(200L);
        pool2.setProceso(proceso);
        pool2.setEmpresa(empresa);

        lane1 = new Lane();
        lane1.setId(1000L);
        lane1.setPool(pool1);
        lane1.setEmpresa(empresa);

        lane2 = new Lane();
        lane2.setId(2000L);
        lane2.setPool(pool1);
        lane2.setEmpresa(empresa);

        lane3 = new Lane();
        lane3.setId(3000L);
        lane3.setPool(pool2);
        lane3.setEmpresa(empresa);

        actividad = new Actividad();
        actividad.setId(1L);
        actividad.setNombre("Tarea A");
        actividad.setDescripcion("Desc");
        actividad.setLane(lane1);
        actividad.setEmpresa(empresa);
        actividad.setPosicionX(0);
        actividad.setPosicionY(0);
    }

    @Test
    @DisplayName("Editar con nombre duplicado en el proceso lanza excepcion")
    void editar_nombre_duplicado() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(actividad));
        when(nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(
                "Tarea B", 10L, 1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> actividadService.editar(1L, 1L, "Tarea B", "Desc", 0, 0, null));
    }

    @Test
    @DisplayName("Editar conservando el mismo nombre no lanza excepcion")
    void editar_mismo_nombre() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(actividad));
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Actividad resultado = actividadService.editar(1L, 1L, "Tarea A", "Desc2", 10, 20, null);

        assertEquals("Tarea A", resultado.getNombre());
        verify(nodoFlujoRepository, never())
                .existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Mover actividad a lane del mismo pool funciona")
    void mover_a_lane_mismo_pool() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(actividad));
        when(laneRepository.findByIdAndEmpresaId(2000L, 1L)).thenReturn(Optional.of(lane2));
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Actividad resultado = actividadService.editar(1L, 1L, "Tarea A", "Desc", 0, 0, 2000L);

        assertEquals(lane2, resultado.getLane());
    }

    @Test
    @DisplayName("Mover actividad a lane de otro pool sin arcos funciona")
    void mover_a_otro_pool_sin_arcos() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(actividad));
        when(laneRepository.findByIdAndEmpresaId(3000L, 1L)).thenReturn(Optional.of(lane3));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(Collections.emptyList());
        when(arcoRepository.findAllByDestinoIdAndEmpresaId(1L, 1L)).thenReturn(Collections.emptyList());
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Actividad resultado = actividadService.editar(1L, 1L, "Tarea A", "Desc", 0, 0, 3000L);

        assertEquals(lane3, resultado.getLane());
    }

    @Test
    @DisplayName("Mover actividad a lane de otro pool con arcos lanza excepcion")
    void mover_a_otro_pool_con_arcos() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(actividad));
        when(laneRepository.findByIdAndEmpresaId(3000L, 1L)).thenReturn(Optional.of(lane3));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(new Arco()));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> actividadService.editar(1L, 1L, "Tarea A", "Desc", 0, 0, 3000L));

        assertTrue(ex.getMessage().contains("arcos conectados"));
    }

    @Test
    @DisplayName("Editar con laneId igual al actual no intenta mover")
    void editar_lane_igual_al_actual() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(actividad));
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Actividad resultado = actividadService.editar(1L, 1L, "Tarea A", "Desc", 5, 10, 1000L);

        assertEquals(lane1, resultado.getLane());
        assertEquals(5, resultado.getPosicionX());
        verify(laneRepository, never()).findByIdAndEmpresaId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Mover a otro pool con arcos como destino lanza excepcion")
    void mover_a_otro_pool_con_arcos_destino() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(actividad));
        when(laneRepository.findByIdAndEmpresaId(3000L, 1L)).thenReturn(Optional.of(lane3));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(Collections.emptyList());
        when(arcoRepository.findAllByDestinoIdAndEmpresaId(1L, 1L)).thenReturn(List.of(new Arco()));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> actividadService.editar(1L, 1L, "Tarea A", "Desc", 0, 0, 3000L));

        assertTrue(ex.getMessage().contains("arcos conectados"));
    }
}
