package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;

@ExtendWith(MockitoExtension.class)
class EventoMensajeServiceTest {

    @Mock
    private NodoFlujoService nodoFlujoService;
    @Mock
    private LaneService laneService;
    @Mock
    private ArcoService arcoService;
    @Mock
    private AuditoriaModeladoService auditoriaModeladoService;

    @InjectMocks
    private EventoMensajeService eventoMensajeService;

    @Test
    void crear_evento_guarda_y_audita() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        Proceso proceso = new Proceso();
        proceso.setId(10L);
        Pool pool = new Pool();
        pool.setId(3L);
        pool.setProceso(proceso);
        pool.setCajaNegra(false);
        Lane lane = new Lane();
        lane.setId(2L);
        lane.setEmpresa(empresa);
        lane.setPool(pool);

        when(laneService.obtener(1L, 2L)).thenReturn(lane);
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L)).thenReturn(List.of());
        when(nodoFlujoService.guardar(any())).thenAnswer(inv -> {
            EventoMensaje e = inv.getArgument(0);
            e.setId(5L);
            return e;
        });

        EventoMensaje resultado = eventoMensajeService.crear(
                1L, 2L, "Orden", TipoEventoMensaje.THROW,
                "payload", "pedidoId", 10, 20, false);

        assertEquals(5L, resultado.getId());
        assertEquals(TipoEventoMensaje.THROW, resultado.getTipoEvento());
        verify(auditoriaModeladoService).registrar(eq(proceso), contains("Evento de mensaje creado"));
    }

    @Test
    void crear_en_pool_caja_negra_falla() {
        Proceso proceso = new Proceso();
        proceso.setId(10L);
        Pool pool = new Pool();
        pool.setCajaNegra(true);
        pool.setProceso(proceso);
        Lane lane = new Lane();
        lane.setPool(pool);

        when(laneService.obtener(1L, 2L)).thenReturn(lane);

        assertThrows(com.facimus.procesos.common.ReglaNegocioException.class, () ->
                eventoMensajeService.crear(1L, 2L, "Orden", TipoEventoMensaje.THROW,
                        "payload", "pedidoId", 0, 0, false));
    }

    @Test
    void eliminar_desactiva_evento_y_arcos() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        Proceso proceso = new Proceso();
        proceso.setId(10L);
        Pool pool = new Pool();
        pool.setProceso(proceso);
        Lane lane = new Lane();
        lane.setPool(pool);

        EventoMensaje evento = new EventoMensaje();
        evento.setId(5L);
        evento.setNombre("Orden");
        evento.setEmpresa(empresa);
        evento.setLane(lane);
        evento.setActivo(true);

        when(nodoFlujoService.buscar(1L, 5L)).thenReturn(Optional.of(evento));

        eventoMensajeService.eliminar(1L, 5L);

        assertFalse(evento.isActivo());
        verify(arcoService).desactivarPorNodo(1L, 5L);
        verify(nodoFlujoService).guardar(evento);
    }

    @Test
    void listar_por_lane_solo_retorna_eventos_activos() {
        EventoMensaje activo = new EventoMensaje();
        activo.setActivo(true);
        Actividad actividad = new Actividad();
        actividad.setActivo(true);
        EventoMensaje inactivo = new EventoMensaje();
        inactivo.setActivo(false);

        Lane lane = new Lane();
        when(laneService.obtener(1L, 2L)).thenReturn(lane);
        when(nodoFlujoService.listarPorLane(1L, 2L))
                .thenReturn(List.of(activo, actividad, inactivo));

        List<EventoMensaje> resultado = eventoMensajeService.listarPorLane(1L, 2L);

        assertEquals(1, resultado.size());
        assertSame(activo, resultado.get(0));
    }


    @Test
    void arco_inactivo_no_bloquea_catch_inicio() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        Proceso proceso = new Proceso();
        proceso.setId(10L);
        Pool pool = new Pool();
        pool.setProceso(proceso);
        Lane lane = new Lane();
        lane.setPool(pool);

        EventoMensaje evento = new EventoMensaje();
        evento.setId(5L);
        evento.setNombre("Recepcion");
        evento.setEmpresa(empresa);
        evento.setLane(lane);
        evento.setActivo(true);
        evento.setTipoEvento(TipoEventoMensaje.CATCH_INTERMEDIO);

        Arco arcoInactivo = new Arco();
        arcoInactivo.setActivo(false);

        when(nodoFlujoService.buscar(1L, 5L)).thenReturn(Optional.of(evento));
        when(arcoService.listarPorDestino(1L, 5L)).thenReturn(List.of(arcoInactivo));
        when(nodoFlujoService.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMensaje resultado = eventoMensajeService.editar(
                1L, 5L, "Recepcion", TipoEventoMensaje.CATCH_INICIO,
                "payload", "pedidoId", 10, 20, false);

        assertEquals(TipoEventoMensaje.CATCH_INICIO, resultado.getTipoEvento());
    }

    private Lane laneDeProceso10() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        Proceso proceso = new Proceso();
        proceso.setId(10L);
        Pool pool = new Pool();
        pool.setProceso(proceso);
        pool.setCajaNegra(false);
        Lane lane = new Lane();
        lane.setEmpresa(empresa);
        lane.setPool(pool);
        return lane;
    }

    private EventoMensaje eventoExistente(String nombre, TipoEventoMensaje tipo) {
        EventoMensaje e = new EventoMensaje();
        e.setId(99L);
        e.setNombre(nombre);
        e.setTipoEvento(tipo);
        e.setActivo(true);
        return e;
    }

    @Test
    void crear_catch_con_mismo_nombre_que_su_throw_es_valido() {
        when(laneService.obtener(1L, 2L)).thenReturn(laneDeProceso10());
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L))
                .thenReturn(List.of(eventoExistente("Pedido confirmado", TipoEventoMensaje.THROW)));
        when(nodoFlujoService.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMensaje resultado = eventoMensajeService.crear(1L, 2L, "pedido confirmado",
                TipoEventoMensaje.CATCH_INICIO, "idPedido: numero", "idPedido", 0, 0, false);

        assertEquals(TipoEventoMensaje.CATCH_INICIO, resultado.getTipoEvento());
    }

    @Test
    void crear_segundo_throw_con_mismo_nombre_falla() {
        when(laneService.obtener(1L, 2L)).thenReturn(laneDeProceso10());
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L))
                .thenReturn(List.of(eventoExistente("Pedido confirmado", TipoEventoMensaje.THROW)));

        assertThrows(com.facimus.procesos.common.ReglaNegocioException.class, () ->
                eventoMensajeService.crear(1L, 2L, "Pedido confirmado", TipoEventoMensaje.THROW,
                        "idPedido: numero", "idPedido", 0, 0, false));
    }

    @Test
    void crear_segundo_catch_con_mismo_nombre_falla() {
        when(laneService.obtener(1L, 2L)).thenReturn(laneDeProceso10());
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L))
                .thenReturn(List.of(eventoExistente("Pedido confirmado", TipoEventoMensaje.CATCH_INTERMEDIO)));

        assertThrows(com.facimus.procesos.common.ReglaNegocioException.class, () ->
                eventoMensajeService.crear(1L, 2L, "Pedido confirmado", TipoEventoMensaje.CATCH_INICIO,
                        "idPedido: numero", "idPedido", 0, 0, false));
    }

    @Test
    void crear_evento_con_nombre_de_una_actividad_falla() {
        Actividad actividad = new Actividad();
        actividad.setId(98L);
        actividad.setNombre("Pedido confirmado");
        actividad.setActivo(true);
        when(laneService.obtener(1L, 2L)).thenReturn(laneDeProceso10());
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L)).thenReturn(List.of(actividad));

        assertThrows(com.facimus.procesos.common.ReglaNegocioException.class, () ->
                eventoMensajeService.crear(1L, 2L, "Pedido confirmado", TipoEventoMensaje.CATCH_INICIO,
                        "idPedido: numero", "idPedido", 0, 0, false));
    }

    private EventoMensaje eventoAEditar(String nombre, TipoEventoMensaje tipo) {
        EventoMensaje e = eventoExistente(nombre, tipo);
        e.setId(5L);
        e.setEmpresa(laneDeProceso10().getEmpresa());
        e.setLane(laneDeProceso10());
        return e;
    }

    @Test
    void editar_renombrar_catch_al_nombre_de_su_throw_es_valido() {
        EventoMensaje catchEvt = eventoAEditar("Recepcion", TipoEventoMensaje.CATCH_INTERMEDIO);
        when(nodoFlujoService.buscar(1L, 5L)).thenReturn(Optional.of(catchEvt));
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L)).thenReturn(List.of(
                catchEvt, eventoExistente("Pedido confirmado", TipoEventoMensaje.THROW)));
        when(nodoFlujoService.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMensaje resultado = eventoMensajeService.editar(1L, 5L, "Pedido confirmado",
                TipoEventoMensaje.CATCH_INTERMEDIO, "p", "k", 0, 0, false);

        assertEquals("Pedido confirmado", resultado.getNombre());
    }

    @Test
    void editar_a_nombre_de_otro_evento_del_mismo_lado_falla() {
        EventoMensaje catchEvt = eventoAEditar("Recepcion", TipoEventoMensaje.CATCH_INTERMEDIO);
        EventoMensaje sinNombre = eventoExistente(null, TipoEventoMensaje.THROW);
        sinNombre.setId(97L);
        when(nodoFlujoService.buscar(1L, 5L)).thenReturn(Optional.of(catchEvt));
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L)).thenReturn(List.of(
                catchEvt, sinNombre, eventoExistente("Otro", TipoEventoMensaje.CATCH_INICIO)));

        assertThrows(com.facimus.procesos.common.ReglaNegocioException.class, () ->
                eventoMensajeService.editar(1L, 5L, "Otro",
                        TipoEventoMensaje.CATCH_INTERMEDIO, "p", "k", 0, 0, false));
    }

    @Test
    void editar_excluye_al_propio_evento_al_validar_nombre() {
        EventoMensaje throwEvt = eventoAEditar("Pedido", TipoEventoMensaje.THROW);
        when(nodoFlujoService.buscar(1L, 5L)).thenReturn(Optional.of(throwEvt));
        when(nodoFlujoService.listarActivosPorProceso(1L, 10L)).thenReturn(List.of(throwEvt));
        when(nodoFlujoService.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMensaje resultado = eventoMensajeService.editar(1L, 5L, "Pedido nuevo",
                TipoEventoMensaje.THROW, "p", "k", 0, 0, false);

        assertEquals("Pedido nuevo", resultado.getNombre());
    }
}
