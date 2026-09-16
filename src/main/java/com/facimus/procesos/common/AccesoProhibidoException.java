package com.facimus.procesos.common;

/**
 * Se lanza cuando un usuario sin los permisos necesarios intenta
 * ejecutar una operacion restringida (ej. solo administrador).
 */
public class AccesoProhibidoException extends RuntimeException {

    public AccesoProhibidoException(String mensaje) {
        super(mensaje);
    }
}
