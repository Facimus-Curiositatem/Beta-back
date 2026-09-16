package com.facimus.procesos.gestion.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.gestion.controller.dto.EmpresaResponse;
import com.facimus.procesos.gestion.controller.dto.RegistroEmpresaRequest;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.service.EmpresaService;

import lombok.RequiredArgsConstructor;

/** HU-01: registro de una nueva empresa y su administrador inicial. */
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<EmpresaResponse> registrar(@Validated @RequestBody RegistroEmpresaRequest request) {
        Empresa empresa = empresaService.registrar(request.nombreEmpresa(), request.nit(),
                request.correoContacto(), request.nombreAdmin(), request.emailAdmin(), request.passwordAdmin());
        return ResponseEntity.status(HttpStatus.CREATED).body(EmpresaResponse.of(empresa));
    }
}
