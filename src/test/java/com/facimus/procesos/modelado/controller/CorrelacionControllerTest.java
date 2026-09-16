package com.facimus.procesos.modelado.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.config.SesionActiva;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.modelado.model.Correlacion;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.service.CorrelacionService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CorrelacionController.class)
class CorrelacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CorrelacionService correlacionService;

    @Test
    @DisplayName("PUT /api/mensajes/{mensajeId}/correlacion - definir correlacion (200)")
    void definir_correlacion() throws Exception {
        Correlacion c = crearCorrelacion(1L, "orderId");
        given(correlacionService.definir(eq(1L), eq(5L), anyString())).willReturn(c);

        mockMvc.perform(put("/api/mensajes/5/correlacion")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"criterio":"orderId"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criterio").value("orderId"))
                .andExpect(jsonPath("$.mensajeId").value(5));
    }

    @Test
    @DisplayName("PUT /api/mensajes/{mensajeId}/correlacion - validacion falla (400)")
    void definir_validacion_falla() throws Exception {
        mockMvc.perform(put("/api/mensajes/5/correlacion")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"criterio":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/mensajes/{mensajeId}/correlacion - obtener correlacion (200)")
    void obtener_correlacion() throws Exception {
        Correlacion c = crearCorrelacion(1L, "customerId");
        given(correlacionService.obtener(1L, 5L)).willReturn(c);

        mockMvc.perform(get("/api/mensajes/5/correlacion").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criterio").value("customerId"));
    }

    @Test
    @DisplayName("GET /api/mensajes/{mensajeId}/correlacion - sin sesion retorna 401")
    void obtener_sin_sesion() throws Exception {
        mockMvc.perform(get("/api/mensajes/5/correlacion"))
                .andExpect(status().isUnauthorized());
    }

    private Correlacion crearCorrelacion(Long id, String criterio) {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(5L);

        Correlacion c = new Correlacion();
        c.setId(id);
        c.setCriterio(criterio);
        c.setMensaje(mensaje);
        return c;
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
