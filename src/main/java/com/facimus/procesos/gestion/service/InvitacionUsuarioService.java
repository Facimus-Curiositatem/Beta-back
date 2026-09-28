package com.facimus.procesos.gestion.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.InvitacionUsuario;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.repository.EmpresaRepository;
import com.facimus.procesos.gestion.repository.InvitacionUsuarioRepository;
import com.facimus.procesos.gestion.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** HU-02: invitacion de colaboradores mediante un enlace temporal. */
@Service
@RequiredArgsConstructor
public class InvitacionUsuarioService {

    private final InvitacionUsuarioRepository invitacionUsuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public InvitacionUsuario crear(Long empresaId, String email, RolAcceso rolAcceso) {
        if (usuarioRepository.existsByEmpresaIdAndEmail(empresaId, email)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo en la empresa.");
        }
        if (invitacionUsuarioRepository.existsByEmpresaIdAndEmailIgnoreCaseAndUsadaFalse(empresaId, email)) {
            throw new ReglaNegocioException("Ya existe una invitacion pendiente para ese correo.");
        }
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada."));

        InvitacionUsuario invitacion = new InvitacionUsuario();
        invitacion.setEmpresa(empresa);
        invitacion.setEmail(email.trim().toLowerCase());
        invitacion.setRolAcceso(rolAcceso);
        invitacion.setToken(UUID.randomUUID().toString());
        invitacion.setFechaExpiracion(LocalDateTime.now().plusHours(48));
        invitacion.setUsada(false);
        return invitacionUsuarioRepository.save(invitacion);
    }

    @Transactional
    public Usuario aceptar(String token, String nombre, String password) {
        InvitacionUsuario invitacion = invitacionUsuarioRepository.findByTokenAndUsadaFalse(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitacion no encontrada o ya utilizada."));
        if (invitacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La invitacion ha expirado.");
        }
        Usuario usuario = usuarioService.crearColaborador(invitacion.getEmpresa().getId(), nombre,
                invitacion.getEmail(), password, invitacion.getRolAcceso());
        invitacion.setUsada(true);
        invitacionUsuarioRepository.save(invitacion);
        return usuario;
    }
}
