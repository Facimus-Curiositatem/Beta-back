package com.facimus.procesos.modelado.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Evento BPMN de mensaje: lanzamiento o recepcion. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("EVENTO_MENSAJE")
public class EventoMensaje extends NodoFlujo {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento_mensaje")
    private TipoEventoMensaje tipoEvento;

    @Column(name = "contenido_evento", columnDefinition = "text")
    private String contenido;

    @Column(name = "clave_correlacion_evento")
    private String claveCorrelacion;

    @Column(name = "origen_externo")
    private boolean origenExterno;
}
