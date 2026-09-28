package com.facimus.procesos.modelado.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
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

    @Lob
    @Column(name = "contenido_evento")
    private String contenido;

    @Column(name = "clave_correlacion_evento")
    private String claveCorrelacion;

    // Boolean (no boolean primitivo): con SINGLE_TABLE los demas subtipos comparten esta columna
    // y no tienen este campo. Un primitivo hace que Hibernate infiera NOT NULL y genere un check
    // constraint condicional fragil (tipo_nodo <> 'EVENTO_MENSAJE' OR origen_externo IS NOT NULL)
    // que fallaba de forma intermitente en la insercion.
    @Column(name = "origen_externo")
    private Boolean origenExterno;
}
