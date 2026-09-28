package com.facimus.procesos.gestion.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.gestion.controller.dto.AceptarInvitacionRequest;
import com.facimus.procesos.gestion.controller.dto.InvitacionUsuarioRequest;
import com.facimus.procesos.gestion.controller.dto.InvitacionUsuarioResponse;
import com.facimus.procesos.gestion.controller.dto.UsuarioResponse;
import com.facimus.procesos.gestion.service.InvitacionUsuarioService;
import com.facimus.procesos.security.ApiPrincipal;

import lombok.RequiredArgsConstructor;

/** HU-02: invitacion y alta de colaboradores. */
@RestController
@RequestMapping("/api/v1/usuarios/invitaciones")
@RequiredArgsConstructor
public class InvitacionUsuarioController {

    private final InvitacionUsuarioService invitacionUsuarioService;

    @PostMapping
    public ResponseEntity<InvitacionUsuarioResponse> crear(
            @Validated @RequestBody InvitacionUsuarioRequest request,
            @AuthenticationPrincipal ApiPrincipal principal) {
        var invitacion = invitacionUsuarioService.crear(principal.empresaId(), request.email(), request.rolAcceso());
        return ResponseEntity.created(URI.create("/api/v1/usuarios/invitaciones/" + invitacion.getId()))
                .body(InvitacionUsuarioResponse.of(invitacion));
    }

    @PostMapping("/{token}/aceptar")
    public ResponseEntity<UsuarioResponse> aceptar(@PathVariable String token,
            @Validated @RequestBody AceptarInvitacionRequest request) {
        return ResponseEntity.ok(UsuarioResponse.of(
                invitacionUsuarioService.aceptar(token, request.nombre(), request.password())));
    }
}
