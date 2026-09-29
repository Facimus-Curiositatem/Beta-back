package com.facimus.procesos.gestion.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.repository.EmpresaRepository;

/** HU-01: registro de empresa + usuario administrador inicial. */
@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioService usuarioService;

    public EmpresaService(EmpresaRepository empresaRepository, @Lazy UsuarioService usuarioService) {
        this.empresaRepository = empresaRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public Empresa registrar(String nombre, String nit, String correoContacto,
            String nombreAdmin, String emailAdmin, String passwordAdmin) {
        if (empresaRepository.existsByNit(nit)) {
            throw new ReglaNegocioException("Ya existe una empresa registrada con el NIT " + nit + ".");
        }

        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        empresa.setNit(nit);
        empresa.setCorreoContacto(correoContacto);
        empresa.setFechaRegistro(LocalDate.now());
        empresa = empresaRepository.save(empresa);

        usuarioService.crearColaborador(empresa.getId(), nombreAdmin, emailAdmin, passwordAdmin,
                RolAcceso.ADMINISTRADOR);

        return empresa;
    }

    public Empresa obtener(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada."));
    }

    public Optional<Empresa> buscarPorNit(String nit) {
        return empresaRepository.findByNit(nit);
    }
}
