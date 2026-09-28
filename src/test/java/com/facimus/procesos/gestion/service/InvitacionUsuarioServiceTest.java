package com.facimus.procesos.gestion.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.InvitacionUsuario;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.repository.EmpresaRepository;
import com.facimus.procesos.gestion.repository.InvitacionUsuarioRepository;
import com.facimus.procesos.gestion.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InvitacionUsuarioServiceTest {

    @Mock
    private InvitacionUsuarioRepository invitacionUsuarioRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private InvitacionUsuarioService invitacionUsuarioService;

    @Test
    void crear_limpia_invitaciones_expiradas() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);

        when(usuarioRepository.existsByEmpresaIdAndEmail(1L, "nuevo@demo.com")).thenReturn(false);
        when(invitacionUsuarioRepository
                .existsByEmpresaIdAndEmailIgnoreCaseAndUsadaFalseAndFechaExpiracionAfter(
                        any(), any(), any())).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(invitacionUsuarioRepository.save(any(InvitacionUsuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        invitacionUsuarioService.crear(1L, "nuevo@demo.com", RolAcceso.EDITOR);

        verify(invitacionUsuarioRepository)
                .deleteAllByFechaExpiracionBeforeAndUsadaFalse(any());
    }
}
