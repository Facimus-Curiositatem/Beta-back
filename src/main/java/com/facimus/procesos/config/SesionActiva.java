package com.facimus.procesos.config;

import com.facimus.procesos.gestion.model.RolAcceso;

import jakarta.servlet.http.HttpSession;

/**
 * Acceso tipado a los atributos de sesion que guarda el login (empresaId,
 * usuarioId, rolAcceso, nombreUsuario). Evita repetir casts y nombres de
 * atributo "a mano" en cada controlador.
 */
public final class SesionActiva {

    public static final String EMPRESA_ID = "empresaId";
    public static final String USUARIO_ID = "usuarioId";
    public static final String ROL_ACCESO = "rolAcceso";
    public static final String NOMBRE_USUARIO = "nombreUsuario";
    public static final String NOMBRE_EMPRESA = "nombreEmpresa";

    private SesionActiva() {
    }

    public static boolean haySesion(HttpSession session) {
        return session != null && session.getAttribute(EMPRESA_ID) != null;
    }

    public static Long empresaId(HttpSession session) {
        return (Long) session.getAttribute(EMPRESA_ID);
    }

    public static Long usuarioId(HttpSession session) {
        return (Long) session.getAttribute(USUARIO_ID);
    }

    public static RolAcceso rolAcceso(HttpSession session) {
        return (RolAcceso) session.getAttribute(ROL_ACCESO);
    }

    public static boolean esAdministrador(HttpSession session) {
        return rolAcceso(session) == RolAcceso.ADMINISTRADOR;
    }

    public static boolean puedeEditar(HttpSession session) {
        RolAcceso rol = rolAcceso(session);
        return rol == RolAcceso.ADMINISTRADOR || rol == RolAcceso.EDITOR;
    }
}
