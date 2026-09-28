package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoGateway;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
class ArcoServiceTest {

    @Mock
    private ArcoRepository arcoRepository;
    @Mock
    private NodoFlujoRepository nodoFlujoRepository;
    @Mock
    private PoolRepository poolRepository;

    @InjectMocks
    private ArcoService arcoService;

    private Empresa empresa;
    private Pool pool;
    private Lane lane;
    private Actividad nodoA;
    private Actividad nodoB;
    private Gateway gatewayExclusivo;
    private Arco arco;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);

        pool = new Pool();
        pool.setId(100L);
        pool.setEmpresa(empresa);

        lane = new Lane();
        lane.setId(1000L);
        lane.setPool(pool);

        nodoA = new Actividad();
        nodoA.setId(10L);
        nodoA.setLane(lane);
        nodoA.setEmpresa(empresa);

        nodoB = new Actividad();
        nodoB.setId(20L);
        nodoB.setLane(lane);
        nodoB.setEmpresa(empresa);

        gatewayExclusivo = new Gateway();
        gatewayExclusivo.setId(30L);
        gatewayExclusivo.setTipoGateway(TipoGateway.EXCLUSIVO);
        gatewayExclusivo.setLane(lane);
        gatewayExclusivo.setEmpresa(empresa);

        arco = new Arco();
        arco.setId(1L);
        arco.setOrigen(nodoA);
        arco.setDestino(nodoB);
        arco.setPool(pool);
        arco.setEmpresa(empresa);
        arco.setEtiqueta("si");
        arco.setCondicion(null);
    }

    @Test
    @DisplayName("Crear arco saliente de gateway EXCLUSIVO sin condicion lanza excepcion")
    void crear_arco_desde_gateway_exclusivo_sin_condicion() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(30L, 1L)).thenReturn(Optional.of(gatewayExclusivo));
        when(nodoFlujoRepository.findByIdAndEmpresaId(20L, 1L)).thenReturn(Optional.of(nodoB));
        when(arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(30L, 20L, 1L)).thenReturn(false);

        assertThrows(ReglaNegocioException.class,
                () -> arcoService.crear(1L, 30L, 20L, "si", null));
    }

    @Test
    @DisplayName("Crear arco saliente de gateway EXCLUSIVO con condicion funciona")
    void crear_arco_desde_gateway_exclusivo_con_condicion() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(30L, 1L)).thenReturn(Optional.of(gatewayExclusivo));
        when(nodoFlujoRepository.findByIdAndEmpresaId(20L, 1L)).thenReturn(Optional.of(nodoB));
        when(arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(30L, 20L, 1L)).thenReturn(false);
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.crear(1L, 30L, 20L, "si", "monto > 100");

        assertEquals("monto > 100", resultado.getCondicion());
    }

    @Test
    @DisplayName("Crear arco hacia gateway EXCLUSIVO sin condicion NO lanza excepcion")
    void crear_arco_hacia_gateway_exclusivo_sin_condicion() {
        when(nodoFlujoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(nodoA));
        when(nodoFlujoRepository.findByIdAndEmpresaId(30L, 1L)).thenReturn(Optional.of(gatewayExclusivo));
        when(arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(10L, 30L, 1L)).thenReturn(false);
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.crear(1L, 10L, 30L, "entrada", null);

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Editar arco cambiando origen funciona")
    void editar_cambiar_origen() {
        Actividad nodoC = new Actividad();
        nodoC.setId(40L);
        nodoC.setLane(lane);

        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(nodoFlujoRepository.findByIdAndEmpresaId(40L, 1L)).thenReturn(Optional.of(nodoC));
        when(arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(40L, 20L, 1L)).thenReturn(false);
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.editar(1L, 1L, "si", null, 40L, null);

        assertEquals(nodoC, resultado.getOrigen());
        assertEquals(nodoB, resultado.getDestino());
    }

    @Test
    @DisplayName("Editar arco poniendo origen = destino lanza excepcion")
    void editar_origen_igual_destino() {
        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));

        assertThrows(ReglaNegocioException.class,
                () -> arcoService.editar(1L, 1L, "si", null, 20L, null));
    }

    @Test
    @DisplayName("Editar arco sin cambiar nodos actualiza solo etiqueta y condicion")
    void editar_sin_cambiar_nodos() {
        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.editar(1L, 1L, "nueva etiqueta", null, null, null);

        assertEquals("nueva etiqueta", resultado.getEtiqueta());
        assertNull(resultado.getCondicion());
        assertEquals(nodoA, resultado.getOrigen());
        assertEquals(nodoB, resultado.getDestino());
    }

    @Test
    @DisplayName("Editar arco cambiando solo destino funciona")
    void editar_cambiar_solo_destino() {
        Actividad nodoC = new Actividad();
        nodoC.setId(40L);
        nodoC.setLane(lane);

        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(nodoFlujoRepository.findByIdAndEmpresaId(40L, 1L)).thenReturn(Optional.of(nodoC));
        when(arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(10L, 40L, 1L)).thenReturn(false);
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.editar(1L, 1L, "si", null, null, 40L);

        assertEquals(nodoA, resultado.getOrigen());
        assertEquals(nodoC, resultado.getDestino());
    }

    @Test
    @DisplayName("Editar arco cambiando ambos nodos funciona")
    void editar_cambiar_ambos_nodos() {
        Actividad nodoC = new Actividad();
        nodoC.setId(40L);
        nodoC.setLane(lane);
        Actividad nodoD = new Actividad();
        nodoD.setId(50L);
        nodoD.setLane(lane);

        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(nodoFlujoRepository.findByIdAndEmpresaId(40L, 1L)).thenReturn(Optional.of(nodoC));
        when(nodoFlujoRepository.findByIdAndEmpresaId(50L, 1L)).thenReturn(Optional.of(nodoD));
        when(arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(40L, 50L, 1L)).thenReturn(false);
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.editar(1L, 1L, "si", null, 40L, 50L);

        assertEquals(nodoC, resultado.getOrigen());
        assertEquals(nodoD, resultado.getDestino());
    }

    @Test
    @DisplayName("Editar arco a pools diferentes lanza excepcion")
    void editar_pool_diferente() {
        Pool pool2 = new Pool();
        pool2.setId(200L);
        Lane lane2 = new Lane();
        lane2.setId(2000L);
        lane2.setPool(pool2);

        Actividad nodoC = new Actividad();
        nodoC.setId(40L);
        nodoC.setLane(lane2);

        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(nodoFlujoRepository.findByIdAndEmpresaId(40L, 1L)).thenReturn(Optional.of(nodoC));

        assertThrows(ReglaNegocioException.class,
                () -> arcoService.editar(1L, 1L, "si", null, null, 40L));
    }

    @Test
    @DisplayName("Editar arco duplicado lanza excepcion")
    void editar_arco_duplicado() {
        Actividad nodoC = new Actividad();
        nodoC.setId(40L);
        nodoC.setLane(lane);

        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(nodoFlujoRepository.findByIdAndEmpresaId(40L, 1L)).thenReturn(Optional.of(nodoC));
        when(arcoRepository.existsByOrigenIdAndDestinoIdAndEmpresaId(10L, 40L, 1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> arcoService.editar(1L, 1L, "si", null, null, 40L));
    }

    @Test
    @DisplayName("Editar arco con origen gateway exclusivo sin condicion lanza excepcion")
    void editar_gateway_origen_sin_condicion() {
        arco.setOrigen(gatewayExclusivo);
        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));

        assertThrows(ReglaNegocioException.class,
                () -> arcoService.editar(1L, 1L, "si", null, null, null));
    }

    @Test
    @DisplayName("Editar arco con origen gateway inclusivo con condicion funciona")
    void editar_gateway_inclusivo_origen_con_condicion() {
        Gateway gatewayInclusivo = new Gateway();
        gatewayInclusivo.setId(35L);
        gatewayInclusivo.setTipoGateway(TipoGateway.INCLUSIVO);
        gatewayInclusivo.setLane(lane);
        gatewayInclusivo.setEmpresa(empresa);

        arco.setOrigen(gatewayInclusivo);
        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.editar(1L, 1L, "si", "monto > 50", null, null);

        assertEquals("monto > 50", resultado.getCondicion());
    }

    @Test
    @DisplayName("Editar arco con origenId igual al actual no verifica duplicados")
    void editar_mismo_origen_no_verifica_duplicado() {
        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));
        when(nodoFlujoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(nodoA));
        when(arcoRepository.save(any(Arco.class))).thenAnswer(inv -> inv.getArgument(0));

        Arco resultado = arcoService.editar(1L, 1L, "nueva", null, 10L, null);

        assertEquals("nueva", resultado.getEtiqueta());
        verify(arcoRepository, never()).existsByOrigenIdAndDestinoIdAndEmpresaId(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Editar arco poniendo destino = origen via destinoId lanza excepcion")
    void editar_destino_igual_origen() {
        when(arcoRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(arco));

        assertThrows(ReglaNegocioException.class,
                () -> arcoService.editar(1L, 1L, "si", null, null, 10L));
    }
}
