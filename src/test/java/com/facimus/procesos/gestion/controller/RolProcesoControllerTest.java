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
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.gestion.service.RolProcesoService;
import com.facimus.procesos.gestion.service.dto.RolProcesoVista;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RolProcesoController.class)
class RolProcesoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RolProcesoService rolProcesoService;

    @Test
    @DisplayName("GET /api/roles - listar roles (200)")
    void listar_roles() throws Exception {
        RolProceso rol = crearRol(1L, "Analista", "Analiza procesos");
        RolProcesoVista vista = new RolProcesoVista(rol, 3, true);
        given(rolProcesoService.listarConUso(1L)).willReturn(List.of(vista));

        mockMvc.perform(get("/api/roles").session(sesionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Analista"))
                .andExpect(jsonPath("$[0].procesosQueLoUsan").value(3));
    }

    @Test
    @DisplayName("GET /api/roles - sin sesion retorna 401")
    void listar_sin_sesion() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/roles - crear rol como admin (201)")
    void crear_rol() throws Exception {
        RolProceso rol = crearRol(2L, "Supervisor", "Supervisa");
        given(rolProcesoService.crear(eq(1L), anyString(), anyString())).willReturn(rol);

        mockMvc.perform(post("/api/roles")
                        .session(sesionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Supervisor","descripcion":"Supervisa"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Supervisor"));
    }

    @Test
    @DisplayName("POST /api/roles - editor no puede crear (403)")
    void crear_como_editor() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .session(sesionConRol(RolAcceso.EDITOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"X","descripcion":"Y"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/roles/{id} - editar rol (200)")
    void editar_rol() throws Exception {
        RolProceso rol = crearRol(1L, "Analista Sr", "Senior");
        given(rolProcesoService.editar(eq(1L), eq(1L), anyString(), anyString())).willReturn(rol);

        mockMvc.perform(put("/api/roles/1")
                        .session(sesionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Analista Sr","descripcion":"Senior"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Analista Sr"));
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} - eliminar rol (204)")
    void eliminar_rol() throws Exception {
        doNothing().when(rolProcesoService).eliminar(1L, 1L);

        mockMvc.perform(delete("/api/roles/1").session(sesionAdmin()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/roles - validacion falla sin nombre (400)")
    void crear_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .session(sesionAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","descripcion":"algo"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private RolProceso crearRol(Long id, String nombre, String descripcion) {
        RolProceso rol = new RolProceso();
        rol.setId(id);
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rol.setActivo(true);
        return rol;
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
