package com.facimus.procesos.modelado.controller;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.service.MensajeService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MensajeController.class)
class MensajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MensajeService mensajeService;

    @Test
    @DisplayName("GET /api/procesos/{procesoId}/mensajes - listar mensajes (200)")
    void listar_mensajes() throws Exception {
        Mensaje m = crearMensaje(1L, "Orden de compra");
        given(mensajeService.listarPorProceso(1L, 10L)).willReturn(List.of(m));

        mockMvc.perform(get("/api/procesos/10/mensajes").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Orden de compra"));
    }

    @Test
    @DisplayName("POST /api/procesos/{procesoId}/mensajes - crear mensaje (201)")
    void crear_mensaje() throws Exception {
        Mensaje m = crearMensaje(2L, "Factura");
        given(mensajeService.crear(eq(1L), eq(10L), anyString(), anyString(), anyLong(), anyLong())).willReturn(m);

        mockMvc.perform(post("/api/procesos/10/mensajes")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Factura","contenido":"Datos de factura","poolOrigenId":1,"poolDestinoId":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Factura"));
    }

    @Test
    @DisplayName("POST /api/procesos/{procesoId}/mensajes - validacion falla (400)")
    void crear_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/procesos/10/mensajes")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","contenido":"","poolOrigenId":null,"poolDestinoId":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/mensajes/{id} - editar mensaje (200)")
    void editar_mensaje() throws Exception {
        Mensaje m = crearMensaje(1L, "Orden actualizada");
        given(mensajeService.editar(eq(1L), eq(1L), anyString(), anyString())).willReturn(m);

        mockMvc.perform(put("/api/mensajes/1")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Orden actualizada","contenido":"Nuevo contenido"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Orden actualizada"));
    }

    @Test
    @DisplayName("DELETE /api/mensajes/{id} - eliminar mensaje (204)")
    void eliminar_mensaje() throws Exception {
        doNothing().when(mensajeService).eliminar(1L, 1L);

        mockMvc.perform(delete("/api/mensajes/1").session(sesion()))
                .andExpect(status().isNoContent());
    }

    private Mensaje crearMensaje(Long id, String nombre) {
        Pool poolOrigen = new Pool();
        poolOrigen.setId(1L);

        Pool poolDestino = new Pool();
        poolDestino.setId(2L);

        Proceso proceso = new Proceso();
        proceso.setId(10L);

        Mensaje m = new Mensaje();
        m.setId(id);
        m.setNombre(nombre);
        m.setContenido("Contenido test");
        m.setPoolOrigen(poolOrigen);
        m.setPoolDestino(poolDestino);
        m.setProceso(proceso);
        return m;
    }

    private MockHttpSession sesion() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SesionActiva.EMPRESA_ID, 1L);
        session.setAttribute(SesionActiva.USUARIO_ID, 1L);
        session.setAttribute(SesionActiva.ROL_ACCESO, RolAcceso.EDITOR);
        session.setAttribute(SesionActiva.NOMBRE_USUARIO, "Test");
        session.setAttribute(SesionActiva.NOMBRE_EMPRESA, "Acme");
        return session;
    }
}
