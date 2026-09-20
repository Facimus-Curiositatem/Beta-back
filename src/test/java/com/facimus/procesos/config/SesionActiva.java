package com.facimus.procesos.config;

/**
 * Nombres de los atributos de la MockHttpSession con la que los tests de controllers indican la identidad.
 * SesionDePruebaFilter los convierte en ApiPrincipal: la aplicacion ya no usa sesion.
 */
// ponytail: se borra cuando los tests de controllers dejen MockHttpSession y autentiquen con ApiPrincipal.
public final class SesionActiva {

    public static final String EMPRESA_ID = "empresaId";
    public static final String USUARIO_ID = "usuarioId";
    public static final String ROL_ACCESO = "rolAcceso";
    public static final String NOMBRE_USUARIO = "nombreUsuario";
    public static final String NOMBRE_EMPRESA = "nombreEmpresa";

    private SesionActiva() {
    }
}
