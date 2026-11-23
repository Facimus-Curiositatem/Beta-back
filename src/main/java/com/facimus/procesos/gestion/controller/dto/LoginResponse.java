package com.facimus.procesos.gestion.controller.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
        UsuarioResponse usuario) {
}