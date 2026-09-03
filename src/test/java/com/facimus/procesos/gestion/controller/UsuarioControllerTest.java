package com.facimus.procesos.gestion.controller;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("GET /api/usuarios - listar como admin (200)")
    void listar_como_admin() throws Exception {
        Usuario u = crearUsuario(1L, "Ana", "ana@acme.com", RolAcceso.EDITOR);
        given(usuarioService.listarPorEmpresa(1L)).willReturn(List.of(u));

        mockMvc.perform(get("/api/usuarios").session(sesionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    @Test
    @DisplayName("GET /api/usuarios - sin sesion retorna 401")
    void listar_sin_sesion() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/usuarios - solo lectura retorna 403")
    void listar_solo_lectura() throws Exception {
        mockMvc.perform(get("/api/usuarios").session(sesionConRol(RolAcceso.SOLO_LECTURA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/usuarios - crear colaborador como admin (201)")
    void crear_colaborador() throws Exception {
        Usuario u = crearUsuario(2L, "Pedro", "pedro@acme.com", RolAcceso.EDITOR);
        given(usuarioService.crearColaborador(eq(1L), anyString(), anyString(), anyString(), any()))
                .willReturn(u);

        mockMvc.perform(post("/api/usuarios")
                        .session(sesionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Pedro",
                                  "email": "pedro@acme.com",
                                  "password": "secret123",
                                  "rolAcceso": "EDITOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Pedro"));
    }

    @Test
    @DisplayName("POST /api/usuarios - validacion falla (400)")
    void crear_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .session(sesionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","email":"invalido","password":"12","rolAcceso":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} - obtener usuario (200)")
    void obtener_usuario() throws Exception {
        Usuario u = crearUsuario(5L, "Laura", "laura@acme.com", RolAcceso.EDITOR);
        given(usuarioService.obtener(1L, 5L)).willReturn(u);

        mockMvc.perform(get("/api/usuarios/5").session(sesionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id}/rol - cambiar rol (200)")
    void cambiar_rol() throws Exception {
        Usuario u = crearUsuario(5L, "Laura", "laura@acme.com", RolAcceso.ADMINISTRADOR);
        given(usuarioService.cambiarRolAcceso(1L, 5L, RolAcceso.ADMINISTRADOR)).willReturn(u);

        mockMvc.perform(put("/api/usuarios/5/rol")
                        .session(sesionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rolAcceso":"ADMINISTRADOR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolAcceso").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} - desactivar usuario (204)")
    void desactivar_usuario() throws Exception {
        doNothing().when(usuarioService).desactivar(1L, 5L);

        mockMvc.perform(delete("/api/usuarios/5").session(sesionAdmin()))
                .andExpect(status().isNoContent());
    }

    private Usuario crearUsuario(Long id, String nombre, String email, RolAcceso rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRolAcceso(rol);
        u.setActivo(true);
        return u;
    }

    private MockHttpSession sesionAdmin() {
        return sesionConRol(RolAcceso.ADMINISTRADOR);
    }

    private MockHttpSession sesionConRol(RolAcceso rol) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SesionActiva.EMPRESA_ID, 1L);
        session.setAttribute(SesionActiva.USUARIO_ID, 1L);
        session.setAttribute(SesionActiva.ROL_ACCESO, rol);
        session.setAttribute(SesionActiva.NOMBRE_USUARIO, "Test");
        session.setAttribute(SesionActiva.NOMBRE_EMPRESA, "Acme");
        return session;
    }
}
