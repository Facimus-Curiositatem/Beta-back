package com.facimus.procesos.config;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sustituto minimalista de Spring Security para la Entrega 1: exige que
 * exista sesion activa para cualquier ruta que no sea publica.
 */
public class AutenticacionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (SesionActiva.haySesion(request.getSession(true))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", request.getContextPath() + "/login");
        return false;
    }
}
