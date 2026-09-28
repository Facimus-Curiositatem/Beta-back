package com.facimus.procesos.modelado.controller.dto;

import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;

public record EventoMensajeResponse(Long id, String nombre, TipoEventoMensaje tipoEvento, String contenido,
        String claveCorrelacion, int posicionX, int posicionY, Long laneId, boolean origenExterno) {

    public static EventoMensajeResponse of(EventoMensaje evento) {
        return new EventoMensajeResponse(evento.getId(), evento.getNombre(), evento.getTipoEvento(),
                evento.getContenido(), evento.getClaveCorrelacion(), evento.getPosicionX(), evento.getPosicionY(),
                evento.getLane().getId(), evento.isOrigenExterno());
    }
}
