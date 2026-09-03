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
import com.facimus.procesos.modelado.model.Gateway;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.TipoGateway;
import com.facimus.procesos.modelado.service.GatewayService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GatewayController.class)
class GatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GatewayService gatewayService;

    @Test
    @DisplayName("POST /api/lanes/{laneId}/gateways - crear gateway (201)")
    void crear_gateway() throws Exception {
        Gateway gw = crearGateway(1L, "Decision pago");
        given(gatewayService.crear(eq(1L), eq(3L), anyString(), any(), anyInt(), anyInt())).willReturn(gw);

        mockMvc.perform(post("/api/lanes/3/gateways")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Decision pago","tipoGateway":"EXCLUSIVO","posicionX":300,"posicionY":150}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Decision pago"))
                .andExpect(jsonPath("$.tipoGateway").value("EXCLUSIVO"));
    }

    @Test
    @DisplayName("POST /api/lanes/{laneId}/gateways - validacion falla (400)")
    void crear_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/lanes/3/gateways")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","tipoGateway":null,"posicionX":0,"posicionY":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/gateways/{id} - editar gateway (200)")
    void editar_gateway() throws Exception {
        Gateway gw = crearGateway(1L, "Decision envio");
        gw.setTipoGateway(TipoGateway.PARALELO);
        given(gatewayService.editar(eq(1L), eq(1L), anyString(), any(), anyInt(), anyInt())).willReturn(gw);

        mockMvc.perform(put("/api/gateways/1")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Decision envio","tipoGateway":"PARALELO","posicionX":300,"posicionY":150}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoGateway").value("PARALELO"));
    }

    @Test
    @DisplayName("DELETE /api/gateways/{id} - eliminar gateway (204)")
    void eliminar_gateway() throws Exception {
        doNothing().when(gatewayService).eliminar(1L, 1L);

        mockMvc.perform(delete("/api/gateways/1").session(sesion()))
                .andExpect(status().isNoContent());
    }

    private Gateway crearGateway(Long id, String nombre) {
        Lane lane = new Lane();
        lane.setId(3L);

        Gateway gw = new Gateway();
        gw.setId(id);
        gw.setNombre(nombre);
        gw.setTipoGateway(TipoGateway.EXCLUSIVO);
        gw.setPosicionX(300);
        gw.setPosicionY(150);
        gw.setLane(lane);
        return gw;
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
