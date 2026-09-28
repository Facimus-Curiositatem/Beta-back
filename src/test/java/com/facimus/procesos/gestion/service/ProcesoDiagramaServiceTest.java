package com.facimus.procesos.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.modelado.model.*;
import com.facimus.procesos.modelado.repository.*;

@ExtendWith(MockitoExtension.class)
class ProcesoDiagramaServiceTest {

    @Mock private ProcesoRepository procesoRepository;
    @Mock private PoolRepository poolRepository;
    @Mock private LaneRepository laneRepository;
    @Mock private NodoFlujoRepository nodoFlujoRepository;
    @Mock private ArcoRepository arcoRepository;
    @Mock private MensajeRepository mensajeRepository;

    @InjectMocks
    private ProcesoDiagramaService service;

    @Test
    void obtener_clasifica_nodos_y_excluye_inactivos() {
        Proceso proceso = new Proceso();
        proceso.setId(10L);
        Pool pool = new Pool(); pool.setId(1L); pool.setProceso(proceso);
        Lane lane = new Lane(); lane.setId(2L); lane.setPool(pool);

        Actividad actividad = new Actividad(); actividad.setId(3L); actividad.setActivo(true);
        Gateway gateway = new Gateway(); gateway.setId(4L); gateway.setActivo(true);
        EventoMensaje evento = new EventoMensaje(); evento.setId(5L); evento.setActivo(true);
        Actividad inactiva = new Actividad(); inactiva.setId(6L); inactiva.setActivo(false);

        Arco arco = new Arco(); arco.setId(7L); arco.setActivo(true);
        Arco arcoInactivo = new Arco(); arcoInactivo.setId(8L); arcoInactivo.setActivo(false);
        Mensaje mensaje = new Mensaje(); mensaje.setId(9L);

        when(procesoRepository.findByIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(Optional.of(proceso));
        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(lane));
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L))
                .thenReturn(List.of(actividad,gateway,evento,inactiva));
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(arco,arcoInactivo));
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));

        var detalle = service.obtener(1L, 10L);

        assertEquals(1, detalle.actividades().size());
        assertEquals(1, detalle.gateways().size());
        assertEquals(1, detalle.eventos().size());
        assertEquals(1, detalle.arcos().size());
        assertEquals(1, detalle.mensajes().size());
    }

    @Test
    void proceso_inexistente_falla() {
        when(procesoRepository.findByIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(1L, 10L));
    }
}
