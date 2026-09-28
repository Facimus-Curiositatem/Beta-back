package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.repository.CorrelacionRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;
import com.facimus.procesos.modelado.repository.PoolRepository;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private PoolRepository poolRepository;
    @Mock
    private ProcesoRepository procesoRepository;
    @Mock
    private CorrelacionRepository correlacionRepository;
    @Mock
    private AuditoriaModeladoService auditoriaModeladoService;

    @InjectMocks
    private MensajeService mensajeService;

    private Empresa empresa;
    private Proceso proceso;
    private Pool poolOrigen;
    private Pool poolDestino;
    private Mensaje mensaje;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);

        proceso = new Proceso();
        proceso.setId(10L);
        proceso.setEmpresa(empresa);

        poolOrigen = new Pool();
        poolOrigen.setId(100L);
        poolOrigen.setEmpresa(empresa);
        poolOrigen.setProceso(proceso);

        poolDestino = new Pool();
        poolDestino.setId(200L);
        poolDestino.setEmpresa(empresa);
        poolDestino.setProceso(proceso);

        mensaje = new Mensaje();
        mensaje.setId(1L);
        mensaje.setNombre("Orden");
        mensaje.setContenido("Datos de la orden");
        mensaje.setEmpresa(empresa);
        mensaje.setProceso(proceso);
        mensaje.setPoolOrigen(poolOrigen);
        mensaje.setPoolDestino(poolDestino);
    }

    @Test
    @DisplayName("Crear mensaje entre pools diferentes funciona")
    void crear_exitoso() {
        when(procesoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(proceso));
        when(poolRepository.findByIdAndEmpresaId(100L, 1L)).thenReturn(Optional.of(poolOrigen));
        when(poolRepository.findByIdAndEmpresaId(200L, 1L)).thenReturn(Optional.of(poolDestino));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Mensaje resultado = mensajeService.crear(1L, 10L, "Orden", "Datos", 100L, 200L);

        assertEquals("Orden", resultado.getNombre());
    }

    @Test
    @DisplayName("Crear mensaje con mismo pool origen y destino lanza excepcion")
    void crear_mismo_pool() {
        assertThrows(ReglaNegocioException.class,
                () -> mensajeService.crear(1L, 10L, "Orden", "Datos", 100L, 100L));
    }

    @Test
    @DisplayName("Crear mensaje con proceso inexistente lanza excepcion")
    void crear_proceso_inexistente() {
        when(procesoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> mensajeService.crear(1L, 10L, "Orden", "Datos", 100L, 200L));
    }

    @Test
    @DisplayName("Editar mensaje actualiza nombre y contenido")
    void editar_exitoso() {
        when(mensajeRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(mensaje));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Mensaje resultado = mensajeService.editar(1L, 1L, "Orden v2", "Nuevos datos");

        assertEquals("Orden v2", resultado.getNombre());
        assertEquals("Nuevos datos", resultado.getContenido());
    }

    @Test
    @DisplayName("Eliminar mensaje sin correlacion elimina solo el mensaje")
    void eliminar_sin_correlacion() {
        when(mensajeRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(mensaje));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(1L, 1L)).thenReturn(Optional.empty());

        mensajeService.eliminar(1L, 1L);

        verify(correlacionRepository, never()).delete(any());
        verify(mensajeRepository).delete(mensaje);
    }

    @Test
    @DisplayName("Eliminar mensaje con correlacion elimina ambos")
    void eliminar_con_correlacion() {
        Correlacion correlacion = new Correlacion();
        correlacion.setId(5L);
        when(mensajeRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(mensaje));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(correlacion));

        mensajeService.eliminar(1L, 1L);

        verify(correlacionRepository).delete(correlacion);
        verify(mensajeRepository).delete(mensaje);
    }

    @Test
    @DisplayName("Listar mensajes de proceso inexistente lanza excepcion")
    void listar_proceso_inexistente() {
        when(procesoRepository.existsByIdAndEmpresaId(10L, 1L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class,
                () -> mensajeService.listarPorProceso(1L, 10L));
    }

    @Test
    @DisplayName("Listar mensajes de proceso existente retorna lista")
    void listar_exitoso() {
        when(procesoRepository.existsByIdAndEmpresaId(10L, 1L)).thenReturn(true);
        when(mensajeRepository.findAllByProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(mensaje));

        List<Mensaje> resultado = mensajeService.listarPorProceso(1L, 10L);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Obtener mensaje inexistente lanza excepcion")
    void obtener_inexistente() {
        when(mensajeRepository.findByIdAndEmpresaId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> mensajeService.obtener(1L, 999L));
    }
}
