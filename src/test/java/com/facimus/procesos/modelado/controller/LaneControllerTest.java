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
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.service.LaneService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LaneController.class)
class LaneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LaneService laneService;

    @Test
    @DisplayName("GET /api/pools/{poolId}/lanes - listar lanes (200)")
    void listar_lanes() throws Exception {
        Lane lane = crearLane(1L, "Recepcion");
        given(laneService.listarPorPool(1L, 5L)).willReturn(List.of(lane));

        mockMvc.perform(get("/api/pools/5/lanes").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Recepcion"));
    }

    @Test
    @DisplayName("POST /api/pools/{poolId}/lanes - crear lane (201)")
    void crear_lane() throws Exception {
        Lane lane = crearLane(2L, "Analisis");
        given(laneService.crear(eq(1L), eq(5L), anyString(), anyLong())).willReturn(lane);

        mockMvc.perform(post("/api/pools/5/lanes")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Analisis","rolProcesoId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Analisis"));
    }

    @Test
    @DisplayName("POST /api/pools/{poolId}/lanes - validacion falla (400)")
    void crear_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/pools/5/lanes")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","rolProcesoId":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/lanes/{id} - editar lane (200)")
    void editar_lane() throws Exception {
        Lane lane = crearLane(1L, "Recepcion v2");
        given(laneService.editar(eq(1L), eq(1L), anyString(), anyLong())).willReturn(lane);

        mockMvc.perform(put("/api/lanes/1")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Recepcion v2","rolProcesoId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Recepcion v2"));
    }

    @Test
    @DisplayName("DELETE /api/lanes/{id} - eliminar lane (204)")
    void eliminar_lane() throws Exception {
        doNothing().when(laneService).eliminar(1L, 1L);

        mockMvc.perform(delete("/api/lanes/1").session(sesion()))
                .andExpect(status().isNoContent());
    }

    private Lane crearLane(Long id, String nombre) {
        Pool pool = new Pool();
        pool.setId(5L);

        RolProceso rol = new RolProceso();
        rol.setId(1L);
        rol.setNombre("Analista");

        Lane lane = new Lane();
        lane.setId(id);
        lane.setNombre(nombre);
        lane.setOrden(0);
        lane.setPool(pool);
        lane.setRolProceso(rol);
        return lane;
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
