package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.modelado.model.*;
import com.facimus.procesos.modelado.repository.*;

@ExtendWith(MockitoExtension.class)
class ValidacionModeloServiceTest {

    @Mock private PoolRepository poolRepository;
    @Mock private LaneRepository laneRepository;
    @Mock private NodoFlujoRepository nodoFlujoRepository;
    @Mock private ArcoRepository arcoRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private CorrelacionRepository correlacionRepository;

    @InjectMocks
    private ValidacionModeloService service;

    private Pool pool;
    private Lane lane;

    @BeforeEach
    void setUp() {
        Proceso proceso = new Proceso();
        proceso.setId(10L);

        pool = new Pool();
        pool.setId(100L);
        pool.setNombre("Interno");
        pool.setProceso(proceso);
        pool.setCajaNegra(false);

        lane = new Lane();
        lane.setId(200L);
        lane.setPool(pool);
    }

    @Test
    void modelo_vacio_es_valido() {
        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void caja_negra_con_lane_falla() {
        pool.setCajaNegra(true);
        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(lane));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void gateway_divergente_con_menos_de_dos_salidas_falla() {
        Gateway gateway = new Gateway();
        gateway.setId(1L);
        gateway.setNombre("Decision");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVO);
        gateway.setActivo(true);

        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(gateway));
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void gateway_exclusivo_con_salida_sin_condicion_falla() {
        Gateway gateway = new Gateway();
        gateway.setId(1L);
        gateway.setNombre("Decision");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVO);
        gateway.setActivo(true);

        Actividad a = actividad(2L);
        Actividad b = actividad(3L);
        Arco ab = arco(11L, gateway, a, "");
        Arco ac = arco(12L, gateway, b, "x > 0");

        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(gateway,a,b));
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(ab,ac));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void catch_inicio_con_entrada_falla() {
        EventoMensaje evento = new EventoMensaje();
        evento.setId(5L);
        evento.setNombre("Recepcion");
        evento.setTipoEvento(TipoEventoMensaje.CATCH_INICIO);
        evento.setActivo(true);
        Actividad origen = actividad(6L);
        Arco entrada = arco(20L, origen, evento, null);

        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(evento,origen));
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(entrada));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void mensaje_sin_throw_falla() {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(1L);
        mensaje.setNombre("Orden");
        mensaje.setActivo(true);

        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }


    @Test
    void mensaje_sin_receptor_falla() {
        Mensaje mensaje = mensajeBase();
        mensaje.setEventoThrow(new EventoMensaje());
        mensaje.setClaveCorrelacion("pedidoId");

        prepararEstructuraVacia();
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void mensaje_sin_clave_correlacion_falla() {
        Mensaje mensaje = mensajeBase();
        mensaje.setEventoThrow(new EventoMensaje());
        mensaje.setEventoCatch(new EventoMensaje());

        prepararEstructuraVacia();
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void mensaje_sin_entidad_correlacion_falla() {
        Mensaje mensaje = mensajeBase();
        mensaje.setEventoThrow(new EventoMensaje());
        mensaje.setEventoCatch(new EventoMensaje());
        mensaje.setClaveCorrelacion("pedidoId");

        prepararEstructuraVacia();
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void mensajes_duplicados_por_nombre_y_clave_fallan() {
        Mensaje a = mensajeBase();
        a.setEventoThrow(new EventoMensaje());
        a.setEventoCatch(new EventoMensaje());
        a.setClaveCorrelacion("pedidoId");

        Mensaje b = mensajeBase();
        b.setId(2L);
        b.setNombre(" orden ");
        b.setEventoThrow(new EventoMensaje());
        b.setEventoCatch(new EventoMensaje());
        b.setClaveCorrelacion(" PEDIDOID ");

        Correlacion correlacion = new Correlacion();
        prepararEstructuraVacia();
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(a, b));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(anyLong(), eq(1L)))
                .thenReturn(Optional.of(correlacion));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void mensaje_externo_con_pool_invalido_falla() {
        Mensaje mensaje = mensajeBase();
        mensaje.setEventoThrow(new EventoMensaje());
        mensaje.setClaveCorrelacion("pedidoId");
        mensaje.setTipoDestinoExterno(TipoDestinoExterno.CORREO);
        mensaje.setDestinoExterno("ops@demo.com");
        mensaje.setPoolDestino(pool);

        Correlacion correlacion = new Correlacion();
        prepararEstructuraVacia();
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(correlacion));

        assertThrows(ReglaNegocioException.class, () -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void mensaje_externo_valido_es_aceptado() {
        Pool externo = new Pool();
        externo.setId(200L);
        externo.setTipoParticipante(TipoParticipante.SISTEMA_EXTERNO);
        externo.setCajaNegra(true);

        Mensaje mensaje = mensajeBase();
        mensaje.setEventoThrow(new EventoMensaje());
        mensaje.setClaveCorrelacion("pedidoId");
        mensaje.setTipoDestinoExterno(TipoDestinoExterno.CORREO);
        mensaje.setDestinoExterno("ops@demo.com");
        mensaje.setPoolDestino(externo);

        Correlacion correlacion = new Correlacion();
        prepararEstructuraVacia();
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(correlacion));

        assertDoesNotThrow(() -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void gateway_paralelo_con_dos_salidas_es_valido() {
        Gateway gateway = new Gateway();
        gateway.setId(1L);
        gateway.setNombre("Paralelo");
        gateway.setTipoGateway(TipoGateway.PARALELO);
        gateway.setActivo(true);

        Actividad a = actividad(2L);
        Actividad b = actividad(3L);
        Arco ab = arco(11L, gateway, a, null);
        Arco ac = arco(12L, gateway, b, null);

        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(gateway,a,b));
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(ab,ac));
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.validarParaPublicacion(1L, 10L));
    }

    @Test
    void mensaje_interno_completo_es_valido() {
        EventoMensaje throwEvt = new EventoMensaje();
        throwEvt.setId(7L);
        EventoMensaje catchEvt = new EventoMensaje();
        catchEvt.setId(8L);

        Mensaje mensaje = new Mensaje();
        mensaje.setId(1L);
        mensaje.setNombre("Orden");
        mensaje.setEventoThrow(throwEvt);
        mensaje.setEventoCatch(catchEvt);
        mensaje.setClaveCorrelacion("pedidoId");
        mensaje.setActivo(true);

        Correlacion correlacion = new Correlacion();
        correlacion.setId(4L);

        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(correlacion));

        assertDoesNotThrow(() -> service.validarParaPublicacion(1L, 10L));
    }


    private void prepararEstructuraVacia() {
        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of());
    }

    private Mensaje mensajeBase() {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(1L);
        mensaje.setNombre("Orden");
        mensaje.setActivo(true);
        return mensaje;
    }

    private Actividad actividad(Long id) {
        Actividad a = new Actividad();
        a.setId(id);
        a.setActivo(true);
        return a;
    }

    private Arco arco(Long id, NodoFlujo origen, NodoFlujo destino, String condicion) {
        Arco a = new Arco();
        a.setId(id);
        a.setOrigen(origen);
        a.setDestino(destino);
        a.setCondicion(condicion);
        a.setActivo(true);
        return a;
    }
}
