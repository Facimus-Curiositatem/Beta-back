package com.facimus.procesos.modelado.controller.dto;

import java.util.List;

import com.facimus.procesos.modelado.service.dto.ImpactoEliminacion;

public record ImpactoEliminacionResponse(
        boolean requiereConfirmacion,
        boolean rompeContinuidad,
        List<String> advertencias) {

    public static ImpactoEliminacionResponse of(ImpactoEliminacion impacto) {
        return new ImpactoEliminacionResponse(true, impacto.rompeContinuidad(), impacto.advertencias());
    }
}
