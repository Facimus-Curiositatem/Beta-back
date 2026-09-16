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
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.Arco;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.service.ArcoService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArcoController.class)
class ArcoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArcoService arcoService;

    @Test
    @DisplayName("POST /api/arcos - crear arco (201)")
    void crear_arco() throws Exception {
        Arco arco = crearArco(1L);
        given(arcoService.crear(eq(1L), anyLong(), anyLong(), anyString(), anyString())).willReturn(arco);

        mockMvc.perform(post("/api/arcos")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origenId":10,"destinoId":20,"etiqueta":"si","condicion":"aprobado"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.etiqueta").value("si"));
    }

    @Test
    @DisplayName("POST /api/arcos - sin sesion retorna 401")
    void crear_sin_sesion() throws Exception {
        mockMvc.perform(post("/api/arcos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origenId":10,"destinoId":20,"etiqueta":"si","condicion":"x"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/arcos - validacion falla sin origenId (400)")
    void crear_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/arcos")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origenId":null,"destinoId":null,"etiqueta":"","condicion":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/arcos/{id} - editar arco (200)")
    void editar_arco() throws Exception {
        Arco arco = crearArco(1L);
        arco.setEtiqueta("no");
        given(arcoService.editar(eq(1L), eq(1L), anyString(), anyString())).willReturn(arco);

        mockMvc.perform(put("/api/arcos/1")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etiqueta":"no","condicion":"rechazado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etiqueta").value("no"));
    }

    @Test
    @DisplayName("DELETE /api/arcos/{id} - eliminar arco (204)")
    void eliminar_arco() throws Exception {
        doNothing().when(arcoService).eliminar(1L, 1L);

        mockMvc.perform(delete("/api/arcos/1").session(sesion()))
                .andExpect(status().isNoContent());
    }

    private Arco crearArco(Long id) {
        Pool pool = new Pool();
        pool.setId(5L);

        Lane lane = new Lane();
        lane.setId(3L);

        Actividad origen = new Actividad();
        origen.setId(10L);
        origen.setLane(lane);

        Actividad destino = new Actividad();
        destino.setId(20L);
        destino.setLane(lane);

        Arco arco = new Arco();
        arco.setId(id);
        arco.setEtiqueta("si");
        arco.setCondicion("aprobado");
        arco.setOrigen(origen);
        arco.setDestino(destino);
        arco.setPool(pool);
        return arco;
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
