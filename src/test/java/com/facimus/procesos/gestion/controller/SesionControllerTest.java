package com.facimus.procesos.gestion.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SesionController.class)
class SesionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("POST /api/auth/login - login exitoso (200)")
    void login_exitoso() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Acme Corp");
        empresa.setNit("900123456");
        empresa.setCorreoContacto("info@acme.com");
        empresa.setFechaRegistro(LocalDate.now());

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombre("Juan");
        usuario.setEmail("juan@acme.com");
        usuario.setRolAcceso(RolAcceso.ADMINISTRADOR);
        usuario.setEmpresa(empresa);

        given(usuarioService.autenticar("juan@acme.com", "secret123")).willReturn(usuario);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"juan@acme.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(10))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.rolAcceso").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("POST /api/auth/login - validacion falla (400)")
    void login_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/logout - cierre de sesion (204)")
    void logout_exitoso() throws Exception {
        MockHttpSession session = sesionAutenticada();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());
    }

    private MockHttpSession sesionAutenticada() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SesionActiva.EMPRESA_ID, 1L);
        session.setAttribute(SesionActiva.USUARIO_ID, 1L);
        session.setAttribute(SesionActiva.ROL_ACCESO, RolAcceso.ADMINISTRADOR);
        session.setAttribute(SesionActiva.NOMBRE_USUARIO, "Admin");
        session.setAttribute(SesionActiva.NOMBRE_EMPRESA, "Acme");
        return session;
    }
}
