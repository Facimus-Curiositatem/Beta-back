package com.facimus.procesos.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;

/**
 * Expone los datos de la sesion activa (nombre de usuario, empresa, rol) a
 * todas las vistas, para que el fragmento de cabecera pueda mostrarlos sin
 * que cada controlador tenga que repetirlos.
 */
@ControllerAdvice
public class VistaGlobalAdvice {

    @ModelAttribute("sesionActiva")
    public boolean sesionActiva(HttpSession session) {
        return SesionActiva.haySesion(session);
    }

    @ModelAttribute("nombreUsuarioSesion")
    public String nombreUsuarioSesion(HttpSession session) {
        return (String) session.getAttribute(SesionActiva.NOMBRE_USUARIO);
    }

    @ModelAttribute("nombreEmpresaSesion")
    public String nombreEmpresaSesion(HttpSession session) {
        return (String) session.getAttribute(SesionActiva.NOMBRE_EMPRESA);
    }

    @ModelAttribute("esAdministradorSesion")
    public boolean esAdministradorSesion(HttpSession session) {
        return SesionActiva.haySesion(session) && SesionActiva.esAdministrador(session);
    }

    @ModelAttribute("puedeEditarSesion")
    public boolean puedeEditarSesion(HttpSession session) {
        return SesionActiva.haySesion(session) && SesionActiva.puedeEditar(session);
    }
}
