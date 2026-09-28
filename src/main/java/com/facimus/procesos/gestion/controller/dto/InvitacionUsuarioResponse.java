package com.facimus.procesos.gestion.controller.dto;

import java.time.LocalDateTime;

import com.facimus.procesos.gestion.model.InvitacionUsuario;
import com.facimus.procesos.gestion.model.RolAcceso;

public record InvitacionUsuarioResponse(Long id, String email, RolAcceso rolAcceso,
        String token, LocalDateTime fechaExpiracion) {

    public static InvitacionUsuarioResponse of(InvitacionUsuario invitacion) {
        return new InvitacionUsuarioResponse(invitacion.getId(), invitacion.getEmail(),
                invitacion.getRolAcceso(), invitacion.getToken(), invitacion.getFechaExpiracion());
    }
}
