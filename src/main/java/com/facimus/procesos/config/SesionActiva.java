package com.facimus.procesos.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.security.ApiPrincipal;

import jakarta.servlet.http.HttpSession;

/**
 * Acceso tipado a la identidad del usuario autenticado. Conserva las firmas con HttpSession
 * que usan los controllers, pero los datos salen del ApiPrincipal del JWT.
 */
// ponytail: puente mientras los controllers reciban HttpSession; al migrarlos a ApiPrincipal se elimina esta clase.
public final class SesionActiva {

    public static final String EMPRESA_ID = "empresaId";
    public static final String USUARIO_ID = "usuarioId";
    public static final String ROL_ACCESO = "rolAcceso";
    public static final String NOMBRE_USUARIO = "nombreUsuario";
    public static final String NOMBRE_EMPRESA = "nombreEmpresa";

    private SesionActiva() {
    }

    public static Long empresaId(HttpSession session) {
        ApiPrincipal principal = principal();
        return principal == null ? null : principal.empresaId();
    }

    public static Long usuarioId(HttpSession session) {
        ApiPrincipal principal = principal();
        return principal == null ? null : principal.usuarioId();
    }

    public static RolAcceso rolAcceso(HttpSession session) {
        ApiPrincipal principal = principal();
        return principal == null ? null : principal.rol();
    }

    public static boolean esAdministrador(HttpSession session) {
        return rolAcceso(session) == RolAcceso.ADMINISTRADOR;
    }

    public static boolean puedeEditar(HttpSession session) {
        RolAcceso rol = rolAcceso(session);
        return rol == RolAcceso.ADMINISTRADOR || rol == RolAcceso.EDITOR;
    }

    private static ApiPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof ApiPrincipal principal
                ? principal
                : null;
    }
}
