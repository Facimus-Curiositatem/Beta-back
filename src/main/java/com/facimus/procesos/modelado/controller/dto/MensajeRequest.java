package com.facimus.procesos.modelado.controller.dto;

import com.facimus.procesos.modelado.model.PoliticaFalloNotificacion;
import com.facimus.procesos.modelado.model.PoliticaMensajeSinCaso;
import com.facimus.procesos.modelado.model.TipoDestinoExterno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MensajeRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "El contenido es obligatorio.") String contenido,
        @NotNull(message = "El pool de origen es obligatorio.") Long poolOrigenId,
        @NotNull(message = "El pool de destino es obligatorio.") Long poolDestinoId,
        @NotNull(message = "El Message Throw es obligatorio.") Long eventoThrowId,
        Long eventoCatchId,
        @NotBlank(message = "La clave de correlacion es obligatoria.") String claveCorrelacion,
        TipoDestinoExterno tipoDestinoExterno,
        String destinoExterno,
        PoliticaFalloNotificacion politicaFalloNotificacion,
        Long actividadManejoErrorId,
        PoliticaMensajeSinCaso politicaSinCaso) {
}
