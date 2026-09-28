package com.facimus.procesos.gestion.controller.dto;

import com.facimus.procesos.gestion.service.dto.ProcesoCompartidoDetalle;

public record ProcesoCompartidoDetalleResponse(
        ProcesoResponse proceso,
        String empresaPropietaria,
        boolean soloLectura,
        java.util.List<com.facimus.procesos.modelado.controller.dto.PoolResponse> pools,
        java.util.List<com.facimus.procesos.modelado.controller.dto.LaneResponse> lanes,
        java.util.List<com.facimus.procesos.modelado.controller.dto.ActividadResponse> actividades,
        java.util.List<com.facimus.procesos.modelado.controller.dto.GatewayResponse> gateways,
        java.util.List<com.facimus.procesos.modelado.controller.dto.EventoMensajeResponse> eventos,
        java.util.List<com.facimus.procesos.modelado.controller.dto.ArcoResponse> arcos,
        java.util.List<com.facimus.procesos.modelado.controller.dto.MensajeResponse> mensajes) {

    public static ProcesoCompartidoDetalleResponse of(ProcesoCompartidoDetalle detalle) {
        return new ProcesoCompartidoDetalleResponse(
                ProcesoResponse.of(detalle.proceso()),
                detalle.compartido().getEmpresa().getNombre(),
                true,
                detalle.pools().stream().map(com.facimus.procesos.modelado.controller.dto.PoolResponse::of).toList(),
                detalle.lanes().stream().map(com.facimus.procesos.modelado.controller.dto.LaneResponse::of).toList(),
                detalle.actividades().stream()
                        .map(com.facimus.procesos.modelado.controller.dto.ActividadResponse::of).toList(),
                detalle.gateways().stream()
                        .map(com.facimus.procesos.modelado.controller.dto.GatewayResponse::of).toList(),
                detalle.eventos().stream()
                        .map(com.facimus.procesos.modelado.controller.dto.EventoMensajeResponse::of).toList(),
                detalle.arcos().stream().map(com.facimus.procesos.modelado.controller.dto.ArcoResponse::of).toList(),
                detalle.mensajes().stream()
                        .map(com.facimus.procesos.modelado.controller.dto.MensajeResponse::of).toList());
    }
}
