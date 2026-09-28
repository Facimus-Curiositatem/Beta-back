package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.repository.CorrelacionRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;

@ExtendWith(MockitoExtension.class)
class CorrelacionServiceTest {

    @Mock
    private CorrelacionRepository correlacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private AuditoriaModeladoService auditoriaModeladoService;

    @InjectMocks
    private CorrelacionService correlacionService;


    @Test
    void definir_crea_correlacion_y_actualiza_mensaje() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        Proceso proceso = new Proceso();
        proceso.setId(10L);

        Mensaje mensaje = new Mensaje();
        mensaje.setId(5L);
        mensaje.setNombre("Orden");
        mensaje.setEmpresa(empresa);
        mensaje.setProceso(proceso);

        when(mensajeRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(mensaje));
        when(mensajeRepository.findAllByProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(mensaje));
        when(correlacionRepository.findByMensajeIdAndEmpresaId(5L, 1L)).thenReturn(Optional.empty());
        when(correlacionRepository.save(any(Correlacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Correlacion resultado = correlacionService.definir(1L, 5L, "pedidoId");

        assertEquals("pedidoId", resultado.getCriterio());
        assertEquals("pedidoId", mensaje.getClaveCorrelacion());
        verify(mensajeRepository).save(mensaje);
        verify(auditoriaModeladoService).registrar(eq(proceso), contains("Correlacion actualizada"));
    }

    @Test
    void definir_rechaza_clave_distinta_al_throw() {
        Proceso proceso = new Proceso();
        proceso.setId(10L);
        Mensaje mensaje = new Mensaje();
        mensaje.setId(5L);
        mensaje.setNombre("Orden");
        mensaje.setProceso(proceso);

        EventoMensaje eventoThrow = new EventoMensaje();
        eventoThrow.setClaveCorrelacion("pedidoId");
        mensaje.setEventoThrow(eventoThrow);

        when(mensajeRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(mensaje));
        when(mensajeRepository.findAllByProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(mensaje));

        assertThrows(ReglaNegocioException.class,
                () -> correlacionService.definir(1L, 5L, "otraClave"));
    }

    @Test
    void definir_rechaza_ambiguedad_en_mismo_proceso() {
        Proceso proceso = new Proceso();
        proceso.setId(10L);

        Mensaje mensaje = new Mensaje();
        mensaje.setId(5L);
        mensaje.setNombre("Orden");
        mensaje.setProceso(proceso);

        Mensaje otro = new Mensaje();
        otro.setId(6L);
        otro.setNombre("orden");
        otro.setClaveCorrelacion("pedidoId");
        otro.setProceso(proceso);

        when(mensajeRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(mensaje));
        when(mensajeRepository.findAllByProcesoIdAndEmpresaId(10L, 1L)).thenReturn(List.of(mensaje, otro));

        assertThrows(ReglaNegocioException.class,
                () -> correlacionService.definir(1L, 5L, "pedidoId"));
    }

    @Test
    void obtener_existente_retorna_correlacion() {
        Correlacion correlacion = new Correlacion();
        correlacion.setId(1L);
        when(correlacionRepository.findByMensajeIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(correlacion));

        assertSame(correlacion, correlacionService.obtener(1L, 5L));
    }

    @Test
    void eliminar_tambien_limpia_clave_del_mensaje() {
        Proceso proceso = new Proceso();
        Mensaje mensaje = new Mensaje();
        mensaje.setId(5L);
        mensaje.setNombre("Orden");
        mensaje.setProceso(proceso);
        mensaje.setClaveCorrelacion("pedidoId");

        Correlacion correlacion = new Correlacion();
        correlacion.setId(1L);

        when(correlacionRepository.findByMensajeIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(correlacion));
        when(mensajeRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(mensaje));

        correlacionService.eliminar(1L, 5L);

        assertNull(mensaje.getClaveCorrelacion());
        verify(mensajeRepository).save(mensaje);
        verify(auditoriaModeladoService).registrar(eq(proceso), contains("Correlacion eliminada"));
    }

    @Test
    @DisplayName("Eliminar correlacion existente la borra")
    void eliminar_existente() {
        Correlacion correlacion = new Correlacion();
        correlacion.setId(1L);
        when(correlacionRepository.findByMensajeIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(correlacion));

        correlacionService.eliminar(1L, 5L);

        verify(correlacionRepository).delete(correlacion);
    }

    @Test
    @DisplayName("Eliminar correlacion inexistente lanza excepcion")
    void eliminar_inexistente() {
        when(correlacionRepository.findByMensajeIdAndEmpresaId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> correlacionService.eliminar(1L, 5L));
    }
}
