package com.facimus.procesos.gestion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.controller.dto.LoginRequest;
import com.facimus.procesos.gestion.controller.dto.SesionResponse;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-03: inicio y cierre de sesion. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SesionController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(@Validated @RequestBody LoginRequest request, HttpSession session) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.password());
        session.setAttribute(SesionActiva.EMPRESA_ID, usuario.getEmpresa().getId());
        session.setAttribute(SesionActiva.USUARIO_ID, usuario.getId());
        session.setAttribute(SesionActiva.ROL_ACCESO, usuario.getRolAcceso());
        session.setAttribute(SesionActiva.NOMBRE_USUARIO, usuario.getNombre());
        session.setAttribute(SesionActiva.NOMBRE_EMPRESA, usuario.getEmpresa().getNombre());

        return ResponseEntity.ok(new SesionResponse(
                usuario.getEmpresa().getId(), usuario.getId(), usuario.getNombre(),
                usuario.getEmpresa().getNombre(), usuario.getRolAcceso()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
