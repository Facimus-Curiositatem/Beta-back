package com.facimus.procesos.gestion.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "El refresh token es obligatorio.") String refreshToken) {
}