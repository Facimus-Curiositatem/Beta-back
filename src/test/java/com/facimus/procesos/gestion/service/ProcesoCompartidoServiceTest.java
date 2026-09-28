package com.facimus.procesos.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.ProcesoCompartido;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.repository.EmpresaRepository;
import com.facimus.procesos.gestion.repository.ProcesoCompartidoRepository;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.gestion.repository.UsuarioRepository;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.repository.ArcoRepository;
import com.facimus.procesos.modelado.repository.LaneRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
class ProcesoCompartidoServiceTest {

    @Mock private ProcesoCompartidoRepository procesoCompartidoRepository;
    @Mock private ProcesoRepository procesoRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PoolRepository poolRepository;
    @Mock private LaneRepository laneRepository;
    @Mock private NodoFlujoRepository nodoFlujoRepository;
    @Mock private ArcoRepository arcoRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private HistorialCambioService historialCambioService;

    @InjectMocks
    private ProcesoCompartidoService service;

    private Empresa propietaria;
    private Empresa invitada;
    private Proceso proceso;
    private Usuario autor;

    @BeforeEach
    void setUp() {
        propietaria = new Empresa();
        propietaria.setId(1L);
        propietaria.setNombre("Acme");

        invitada = new Empresa();
        invitada.setId(2L);
        invitada.setNombre("Aliada");

        proceso = new Proceso();
        proceso.setId(10L);
        proceso.setNombre("Compras");
        proceso.setEmpresa(propietaria);
        proceso.setActivo(true);

        autor = new Usuario();
        autor.setId(5L);
        autor.setEmpresa(propietaria);
    }

    @Test
    void compartir_con_si_misma_falla() {
        when(procesoRepository.findByIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(Optional.of(proceso));
        assertThrows(ReglaNegocioException.class, () -> service.compartir(1L, 5L, 10L, 1L));
    }

    @Test
    void compartir_crea_acceso_solo_lectura() {
        when(procesoRepository.findByIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(Optional.of(proceso));
        when(empresaRepository.findById(2L)).thenReturn(Optional.of(invitada));
        when(usuarioRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(autor));
        when(procesoCompartidoRepository.findByProcesoIdAndEmpresaIdAndEmpresaInvitadaId(10L, 1L, 2L))
                .thenReturn(Optional.empty());
        when(procesoCompartidoRepository.save(any(ProcesoCompartido.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcesoCompartido resultado = service.compartir(1L, 5L, 10L, 2L);

        assertTrue(resultado.isActivo());
        assertTrue(resultado.isSoloLectura());
        assertEquals(invitada, resultado.getEmpresaInvitada());
        verify(historialCambioService).registrar(eq(proceso), eq(autor), contains("solo lectura"));
    }

    @Test
    void dejar_de_compartir_desactiva_registro() {
        ProcesoCompartido compartido = new ProcesoCompartido();
        compartido.setEmpresa(propietaria);
        compartido.setProceso(proceso);
        compartido.setEmpresaInvitada(invitada);
        compartido.setActivo(true);

        when(procesoRepository.findByIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(Optional.of(proceso));
        when(usuarioRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(autor));
        when(procesoCompartidoRepository.findByProcesoIdAndEmpresaIdAndEmpresaInvitadaIdAndActivoTrue(10L, 1L, 2L))
                .thenReturn(Optional.of(compartido));

        service.dejarDeCompartir(1L, 5L, 10L, 2L);

        assertFalse(compartido.isActivo());
        verify(procesoCompartidoRepository).save(compartido);
    }

    @Test
    void listar_compartidos_valida_existencia_proceso() {
        when(procesoRepository.existsByIdAndEmpresaId(10L, 1L)).thenReturn(false);
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.listarCompartidosPorPropietario(1L, 10L));
    }

    @Test
    void obtener_compartido_arma_detalle_y_filtra_inactivos() {
        ProcesoCompartido compartido = new ProcesoCompartido();
        compartido.setEmpresa(propietaria);
        compartido.setProceso(proceso);
        compartido.setEmpresaInvitada(invitada);
        compartido.setActivo(true);

        Pool pool = new Pool(); pool.setId(20L); pool.setProceso(proceso);
        Lane lane = new Lane(); lane.setId(21L); lane.setPool(pool);

        Actividad actividad = new Actividad(); actividad.setId(30L); actividad.setActivo(true);
        Gateway gateway = new Gateway(); gateway.setId(31L); gateway.setActivo(true);
        EventoMensaje evento = new EventoMensaje(); evento.setId(32L); evento.setActivo(true);
        Actividad inactiva = new Actividad(); inactiva.setId(33L); inactiva.setActivo(false);

        Arco arco = new Arco(); arco.setId(40L); arco.setActivo(true);
        Arco arcoInactivo = new Arco(); arcoInactivo.setId(41L); arcoInactivo.setActivo(false);
        Mensaje mensaje = new Mensaje(); mensaje.setId(50L);

        when(procesoCompartidoRepository.findByProcesoIdAndEmpresaInvitadaIdAndActivoTrue(10L, 2L))
                .thenReturn(Optional.of(compartido));
        when(poolRepository.findAllByProcesoIdAndEmpresaIdOrderByOrdenAsc(10L, 1L)).thenReturn(List.of(pool));
        when(laneRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(lane));
        when(nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(10L, 1L))
                .thenReturn(List.of(actividad,gateway,evento,inactiva));
        when(arcoRepository.findAllByPool_ProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(arco,arcoInactivo));
        when(mensajeRepository.findAllByProcesoIdAndEmpresaIdAndActivoTrue(10L, 1L)).thenReturn(List.of(mensaje));

        var detalle = service.obtenerCompartido(2L, 10L);

        assertEquals(1, detalle.actividades().size());
        assertEquals(1, detalle.gateways().size());
        assertEquals(1, detalle.eventos().size());
        assertEquals(1, detalle.arcos().size());
        assertEquals(1, detalle.mensajes().size());
    }
}
