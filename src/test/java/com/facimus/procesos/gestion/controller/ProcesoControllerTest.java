package com.facimus.procesos.gestion.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.service.HistorialCambioService;
import com.facimus.procesos.gestion.service.ProcesoService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcesoController.class)
class ProcesoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcesoService procesoService;

    @MockitoBean
    private HistorialCambioService historialCambioService;

    @Test
    @DisplayName("GET /api/procesos - listar procesos (200)")
    void listar_procesos() throws Exception {
        Proceso p = crearProceso(1L, "Ventas");
        Page<Proceso> page = new PageImpl<>(List.of(p));
        given(procesoService.buscar(eq(1L), any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/api/procesos").session(sesionEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Ventas"));
    }

    @Test
    @DisplayName("GET /api/procesos - sin sesion retorna 401")
    void listar_sin_sesion() throws Exception {
        mockMvc.perform(get("/api/procesos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/procesos - crear proceso como editor (201)")
    void crear_proceso() throws Exception {
        Proceso p = crearProceso(2L, "Compras");
        given(procesoService.crear(eq(1L), eq(1L), anyString(), anyString(), anyString())).willReturn(p);

        mockMvc.perform(post("/api/procesos")
                        .session(sesionEditor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Compras","descripcion":"Proceso de compras","categoria":"Operativo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Compras"));
    }

    @Test
    @DisplayName("POST /api/procesos - solo lectura retorna 403")
    void crear_proceso_solo_lectura() throws Exception {
        mockMvc.perform(post("/api/procesos")
                        .session(sesionConRol(RolAcceso.SOLO_LECTURA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"X","descripcion":"Y","categoria":"Z"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/procesos/{id} - detalle proceso (200)")
    void detalle_proceso() throws Exception {
        Proceso p = crearProceso(1L, "Ventas");
        given(procesoService.obtener(1L, 1L)).willReturn(p);
        given(historialCambioService.listarPorProceso(1L, 1L)).willReturn(List.of());

        mockMvc.perform(get("/api/procesos/1").session(sesionEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proceso.nombre").value("Ventas"))
                .andExpect(jsonPath("$.historial").isArray());
    }

    @Test
    @DisplayName("PUT /api/procesos/{id} - editar proceso (200)")
    void editar_proceso() throws Exception {
        Proceso p = crearProceso(1L, "Ventas v2");
        given(procesoService.editar(eq(1L), eq(1L), eq(1L), anyString(), anyString(), anyString(), any()))
                .willReturn(p);

        mockMvc.perform(put("/api/procesos/1")
                        .session(sesionEditor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ventas v2","descripcion":"Desc","categoria":"Op","estado":"BORRADOR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ventas v2"));
    }

    @Test
    @DisplayName("POST /api/procesos/{id}/publicar - publicar proceso (200)")
    void publicar_proceso() throws Exception {
        Proceso p = crearProceso(1L, "Ventas");
        p.setEstado(EstadoProceso.PUBLICADO);
        given(procesoService.publicar(1L, 1L, 1L)).willReturn(p);

        mockMvc.perform(post("/api/procesos/1/publicar").session(sesionEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));
    }

    @Test
    @DisplayName("DELETE /api/procesos/{id} - eliminar como admin (204)")
    void eliminar_proceso() throws Exception {
        doNothing().when(procesoService).eliminarLogico(1L, 1L, 1L);

        mockMvc.perform(delete("/api/procesos/1").session(sesionConRol(RolAcceso.ADMINISTRADOR)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/procesos/{id} - editor no puede eliminar (403)")
    void eliminar_como_editor() throws Exception {
        mockMvc.perform(delete("/api/procesos/1").session(sesionEditor()))
                .andExpect(status().isForbidden());
    }

    private Proceso crearProceso(Long id, String nombre) {
        Proceso p = new Proceso();
        p.setId(id);
        p.setNombre(nombre);
        p.setDescripcion("Descripcion");
        p.setCategoria("Operativo");
        p.setEstado(EstadoProceso.BORRADOR);
        p.setActivo(true);
        p.setFechaCreacion(LocalDateTime.now());
        p.setFechaModificacion(LocalDateTime.now());
        return p;
    }

    private MockHttpSession sesionEditor() {
        return sesionConRol(RolAcceso.EDITOR);
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
