package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.repository.CorrelacionRepository;
import com.facimus.procesos.modelado.repository.MensajeRepository;

@ExtendWith(MockitoExtension.class)
class CorrelacionServiceTest {

    @Mock
    private CorrelacionRepository correlacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;

    @InjectMocks
    private CorrelacionService correlacionService;

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
