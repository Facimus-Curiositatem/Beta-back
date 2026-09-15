package com.facimus.procesos.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.model.RolAcceso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** En los tests de controllers, la MockHttpSession con los atributos de SesionActiva hace las veces del JWT. */
class SesionDePruebaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SesionActiva.EMPRESA_ID) != null) {
            ApiPrincipal principal = new ApiPrincipal(
                    (Long) session.getAttribute(SesionActiva.USUARIO_ID),
                    (Long) session.getAttribute(SesionActiva.EMPRESA_ID),
                    (RolAcceso) session.getAttribute(SesionActiva.ROL_ACCESO),
                    null);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
        }
        filterChain.doFilter(request, response);
    }
}
