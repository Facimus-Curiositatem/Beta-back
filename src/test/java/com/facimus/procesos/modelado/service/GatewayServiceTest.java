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
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoGateway;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;
    @Mock
    private LaneRepository laneRepository;
    @Mock
    private ArcoRepository arcoRepository;
    @Mock
    private AuditoriaModeladoService auditoriaModeladoService;

    @InjectMocks
    private GatewayService gatewayService;

    private Gateway gateway;

    @BeforeEach
    void setUp() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);

        Proceso proceso = new Proceso();
        proceso.setId(10L);
        proceso.setEmpresa(empresa);

        Pool pool = new Pool();
        pool.setId(100L);
        pool.setProceso(proceso);

        Lane lane = new Lane();
        lane.setId(1000L);
        lane.setPool(pool);

        gateway = new Gateway();
        gateway.setId(1L);
        gateway.setNombre("Decision");
        gateway.setTipoGateway(TipoGateway.EXCLUSIVO);
        gateway.setLane(lane);
        gateway.setEmpresa(empresa);
        gateway.setPosicionX(0);
        gateway.setPosicionY(0);
    }


    @Test
    void crear_gateway_exitosamente() {
        Lane lane = gateway.getLane();
        when(laneRepository.findByIdAndEmpresaId(1000L, 1L)).thenReturn(Optional.of(lane));
        when(nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(
                "Nuevo", 10L, 1L)).thenReturn(false);
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Gateway resultado = gatewayService.crear(1L, 1000L, "Nuevo", TipoGateway.EXCLUSIVO, 5, 6);

        assertEquals("Nuevo", resultado.getNombre());
        verify(auditoriaModeladoService).registrar(any(), contains("Gateway creado"));
    }

    @Test
    void crear_en_pool_caja_negra_falla() {
        Lane lane = gateway.getLane();
        lane.getPool().setCajaNegra(true);
        when(laneRepository.findByIdAndEmpresaId(1000L, 1L)).thenReturn(Optional.of(lane));

        assertThrows(ReglaNegocioException.class,
                () -> gatewayService.crear(1L, 1000L, "Nuevo", TipoGateway.EXCLUSIVO, 0, 0));
    }

    @Test
    void impacto_eliminacion_detecta_ramificacion() {
        Arco entrada = new Arco(); entrada.setActivo(true);
        Arco salida1 = new Arco(); salida1.setActivo(true);
        Arco salida2 = new Arco(); salida2.setActivo(true);

        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByDestinoIdAndEmpresaId(1L, 1L)).thenReturn(List.of(entrada));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(salida1, salida2));

        var impacto = gatewayService.evaluarImpactoEliminacion(1L, 1L);

        assertTrue(impacto.rompeContinuidad());
        assertEquals(2, impacto.advertencias().size());
    }

    @Test
    void eliminar_desactiva_gateway_y_arcos() {
        Arco entrada = new Arco(); entrada.setActivo(true);
        Arco salida = new Arco(); salida.setActivo(true);
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByDestinoIdAndEmpresaId(1L, 1L)).thenReturn(List.of(entrada));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(salida));

        gatewayService.eliminar(1L, 1L);

        assertFalse(gateway.isActivo());
        assertFalse(entrada.isActivo());
        assertFalse(salida.isActivo());
        verify(auditoriaModeladoService).registrar(any(), contains("eliminado"));
    }

    @Test
    void listar_por_lane_filtra_gateways_activos() {
        Gateway activo = new Gateway(); activo.setActivo(true);
        Gateway inactivo = new Gateway(); inactivo.setActivo(false);
        com.facimus.procesos.modelado.model.Actividad actividad =
                new com.facimus.procesos.modelado.model.Actividad();
        actividad.setActivo(true);

        when(laneRepository.existsByIdAndEmpresaId(1000L, 1L)).thenReturn(true);
        when(nodoFlujoRepository.findAllByLaneIdAndEmpresaId(1000L, 1L))
                .thenReturn(List.of(activo, inactivo, actividad));

        var resultado = gatewayService.listarPorLane(1L, 1000L);

        assertEquals(1, resultado.size());
        assertSame(activo, resultado.get(0));
    }

    @Test
    @DisplayName("Editar con nombre duplicado lanza excepcion")
    void editar_nombre_duplicado() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaId(
                "Otro", 10L, 1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> gatewayService.editar(1L, 1L, "Otro", TipoGateway.EXCLUSIVO, 0, 0));
    }

    @Test
    @DisplayName("Cambiar de EXCLUSIVO a PARALELO limpia condiciones de arcos salientes")
    void cambiar_a_paralelo_limpia_condiciones() {
        Arco arco = new Arco();
        arco.setCondicion("x > 5");
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(arco));
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        gatewayService.editar(1L, 1L, "Decision", TipoGateway.PARALELO, 0, 0);

        assertNull(arco.getCondicion());
        verify(arcoRepository).save(arco);
    }

    @Test
    @DisplayName("Cambiar de PARALELO a EXCLUSIVO con arcos sin condicion lanza excepcion")
    void cambiar_paralelo_a_exclusivo_sin_condicion() {
        gateway.setTipoGateway(TipoGateway.PARALELO);
        Arco arco = new Arco();
        arco.setCondicion(null);
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(arco));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> gatewayService.editar(1L, 1L, "Decision", TipoGateway.EXCLUSIVO, 0, 0));

        assertTrue(ex.getMessage().contains("arcos salientes sin condicion"));
    }

    @Test
    @DisplayName("Cambiar de PARALELO a EXCLUSIVO con arcos con condicion funciona")
    void cambiar_paralelo_a_exclusivo_con_condicion() {
        gateway.setTipoGateway(TipoGateway.PARALELO);
        Arco arco = new Arco();
        arco.setCondicion("x > 5");
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(arco));
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Gateway resultado = gatewayService.editar(1L, 1L, "Decision", TipoGateway.EXCLUSIVO, 0, 0);

        assertEquals(TipoGateway.EXCLUSIVO, resultado.getTipoGateway());
    }

    @Test
    @DisplayName("Cambiar de PARALELO a INCLUSIVO sin arcos funciona")
    void cambiar_paralelo_a_inclusivo_sin_arcos() {
        gateway.setTipoGateway(TipoGateway.PARALELO);
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(Collections.emptyList());
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Gateway resultado = gatewayService.editar(1L, 1L, "Decision", TipoGateway.INCLUSIVO, 0, 0);

        assertEquals(TipoGateway.INCLUSIVO, resultado.getTipoGateway());
    }

    @Test
    @DisplayName("Cambiar de PARALELO a INCLUSIVO con arcos sin condicion lanza excepcion")
    void cambiar_paralelo_a_inclusivo_sin_condicion() {
        gateway.setTipoGateway(TipoGateway.PARALELO);
        Arco arco = new Arco();
        arco.setCondicion(null);
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(arco));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> gatewayService.editar(1L, 1L, "Decision", TipoGateway.INCLUSIVO, 0, 0));

        assertTrue(ex.getMessage().contains("arcos salientes sin condicion"));
    }

    @Test
    @DisplayName("Editar gateway sin cambiar tipo ni nombre funciona")
    void editar_sin_cambiar_tipo() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Gateway resultado = gatewayService.editar(1L, 1L, "Decision", TipoGateway.EXCLUSIVO, 5, 10);

        assertEquals(TipoGateway.EXCLUSIVO, resultado.getTipoGateway());
        assertEquals(5, resultado.getPosicionX());
    }
    @Test
    @DisplayName("Arcos inactivos no bloquean cambio de PARALELO a EXCLUSIVO")
    void arcos_inactivos_no_bloquean_cambio_tipo() {
        gateway.setTipoGateway(TipoGateway.PARALELO);
        Arco arcoInactivo = new Arco();
        arcoInactivo.setActivo(false);
        arcoInactivo.setCondicion(null);

        when(nodoFlujoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(gateway));
        when(arcoRepository.findAllByOrigenIdAndEmpresaId(1L, 1L)).thenReturn(List.of(arcoInactivo));
        when(nodoFlujoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Gateway resultado = gatewayService.editar(1L, 1L, "Decision", TipoGateway.EXCLUSIVO, 0, 0);

        assertEquals(TipoGateway.EXCLUSIVO, resultado.getTipoGateway());
    }

}
