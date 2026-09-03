package com.facimus.procesos.common;

import java.time.LocalDateTime;

/** Cuerpo estandar de respuesta de error para la API REST. */
public record ErrorResponse(int status, String mensaje, LocalDateTime timestamp) {

    public ErrorResponse(int status, String mensaje) {
        this(status, mensaje, LocalDateTime.now());
    }
}
