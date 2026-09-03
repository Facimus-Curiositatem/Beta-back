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
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.service.PoolService;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PoolController.class)
class PoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PoolService poolService;

    @Test
    @DisplayName("GET /api/procesos/{procesoId}/pools - listar pools (200)")
    void listar_pools() throws Exception {
        Pool pool = crearPool(1L, "Cliente");
        given(poolService.listarPorProceso(1L, 10L)).willReturn(List.of(pool));

        mockMvc.perform(get("/api/procesos/10/pools").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cliente"));
    }

    @Test
    @DisplayName("GET /api/procesos/{procesoId}/pools - sin sesion retorna 401")
    void listar_sin_sesion() throws Exception {
        mockMvc.perform(get("/api/procesos/10/pools"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/procesos/{procesoId}/pools - crear pool (201)")
    void crear_pool() throws Exception {
        Pool pool = crearPool(2L, "Proveedor");
        given(poolService.crear(eq(1L), eq(10L), anyString(), any(), anyBoolean())).willReturn(pool);

        mockMvc.perform(post("/api/procesos/10/pools")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Proveedor","tipoParticipante":"PROVEEDOR","cajaNegra":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Proveedor"));
    }

    @Test
    @DisplayName("POST /api/procesos/{procesoId}/pools - validacion falla (400)")
    void crear_validacion_falla() throws Exception {
        mockMvc.perform(post("/api/procesos/10/pools")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","tipoParticipante":null,"cajaNegra":false}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/pools/{id} - editar pool (200)")
    void editar_pool() throws Exception {
        Pool pool = crearPool(1L, "Cliente VIP");
        given(poolService.editar(eq(1L), eq(1L), anyString(), any())).willReturn(pool);

        mockMvc.perform(put("/api/pools/1")
                        .session(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cliente VIP","tipoParticipante":"CLIENTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cliente VIP"));
    }

    @Test
    @DisplayName("DELETE /api/pools/{id} - eliminar pool (204)")
    void eliminar_pool() throws Exception {
        doNothing().when(poolService).eliminar(1L, 1L);

        mockMvc.perform(delete("/api/pools/1").session(sesion()))
                .andExpect(status().isNoContent());
    }

    private Pool crearPool(Long id, String nombre) {
        Proceso proceso = new Proceso();
        proceso.setId(10L);

        Pool pool = new Pool();
        pool.setId(id);
        pool.setNombre(nombre);
        pool.setTipoParticipante(TipoParticipante.CLIENTE);
        pool.setCajaNegra(false);
        pool.setOrden(0);
        pool.setProceso(proceso);
        return pool;
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
