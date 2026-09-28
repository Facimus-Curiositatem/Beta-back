package com.facimus.procesos.modelado.controller.dto;

import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.PoliticaFalloNotificacion;
import com.facimus.procesos.modelado.model.PoliticaMensajeSinCaso;
import com.facimus.procesos.modelado.model.TipoDestinoExterno;

public record MensajeResponse(Long id, String nombre, String contenido, Long poolOrigenId,
        Long poolDestinoId, Long procesoId, Long eventoThrowId, Long eventoCatchId,
        String claveCorrelacion, TipoDestinoExterno tipoDestinoExterno, String destinoExterno,
        PoliticaFalloNotificacion politicaFalloNotificacion, Long actividadManejoErrorId,
        PoliticaMensajeSinCaso politicaSinCaso, boolean sinReceptor) {

    public static MensajeResponse of(Mensaje m) {
        return new MensajeResponse(m.getId(), m.getNombre(), m.getContenido(), m.getPoolOrigen().getId(),
                m.getPoolDestino().getId(), m.getProceso().getId(),
                m.getEventoThrow() != null ? m.getEventoThrow().getId() : null,
                m.getEventoCatch() != null ? m.getEventoCatch().getId() : null,
                m.getClaveCorrelacion(), m.getTipoDestinoExterno(), m.getDestinoExterno(),
                m.getPoliticaFalloNotificacion(),
                m.getActividadManejoError() != null ? m.getActividadManejoError().getId() : null,
                m.getPoliticaSinCaso(), m.getEventoCatch() == null && m.getTipoDestinoExterno() == null);
    }
}
