package com.facimus.procesos.gestion.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.common.AccesoProhibidoException;
import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.controller.dto.CambiarRolRequest;
import com.facimus.procesos.gestion.controller.dto.CrearUsuarioRequest;
import com.facimus.procesos.gestion.controller.dto.UsuarioResponse;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** HU-02: administracion de colaboradores de la empresa (solo administrador). */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(HttpSession session) {
        exigirAdministrador(session);
        Long empresaId = SesionActiva.empresaId(session);
        List<UsuarioResponse> usuarios = usuarioService.listarPorEmpresa(empresaId).stream()
                .map(UsuarioResponse::of)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Validated @RequestBody CrearUsuarioRequest request,
            HttpSession session) {
        exigirAdministrador(session);
        Long empresaId = SesionActiva.empresaId(session);
        Usuario usuario = usuarioService.crearColaborador(empresaId, request.nombre(), request.email(),
                request.password(), request.rolAcceso());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.of(usuario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id, HttpSession session) {
        Long empresaId = SesionActiva.empresaId(session);
        Usuario usuario = usuarioService.obtener(empresaId, id);
        return ResponseEntity.ok(UsuarioResponse.of(usuario));
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id,
            @Validated @RequestBody CambiarRolRequest request, HttpSession session) {
        exigirAdministrador(session);
        Long empresaId = SesionActiva.empresaId(session);
        Usuario usuario = usuarioService.cambiarRolAcceso(empresaId, id, request.rolAcceso());
        return ResponseEntity.ok(UsuarioResponse.of(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id, HttpSession session) {
        exigirAdministrador(session);
        Long empresaId = SesionActiva.empresaId(session);
        usuarioService.desactivar(empresaId, id);
        return ResponseEntity.noContent().build();
    }

    private void exigirAdministrador(HttpSession session) {
        if (!SesionActiva.esAdministrador(session)) {
            throw new AccesoProhibidoException("Solo un administrador puede realizar esta operacion.");
        }
    }
}
