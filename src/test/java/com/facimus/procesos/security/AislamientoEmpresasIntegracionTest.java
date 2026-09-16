package com.facimus.procesos.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.gestion.controller.dto.LoginRequest;
import com.facimus.procesos.gestion.service.EmpresaService;
import com.facimus.procesos.gestion.service.ProcesoService;
import com.facimus.procesos.gestion.service.RolProcesoService;
import com.facimus.procesos.gestion.service.UsuarioService;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.service.LaneService;
import com.facimus.procesos.modelado.service.MensajeService;
import com.facimus.procesos.modelado.service.PoolService;

import tools.jackson.databind.json.JsonMapper;

/** Aislamiento entre empresas: con el token de la empresa A no se alcanza nada de la empresa B (README §10 y §11). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:aislamiento-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AislamientoEmpresasIntegracionTest {

    private static final String ADMIN_A = "admin@empresa-a.com";
    private static final String ADMIN_B = "admin@empresa-b.com";
    private static final String CLAVE = "clave12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProcesoService procesoService;

    @Autowired
    private RolProcesoService rolProcesoService;

    @Autowired
    private PoolService poolService;

    @Autowired
    private LaneService laneService;

    @Autowired
    private MensajeService mensajeService;

    private String tokenA;
    private String tokenB;
    private Long procesoB;
    private Long poolB;

    @BeforeAll
    void crearDosEmpresas() throws Exception {
        empresaService.registrar("Empresa A", "900100200", "contacto@empresa-a.com", "Admin A", ADMIN_A, CLAVE);
        Long empresaB = empresaService
                .registrar("Empresa B", "900300400", "contacto@empresa-b.com", "Admin B", ADMIN_B, CLAVE).getId();
        Long adminB = usuarioService.autenticar(ADMIN_B, CLAVE).getId();

        procesoB = procesoService.crear(empresaB, adminB, "Compras", "Proceso de compras", "Logistica").getId();
        poolB = poolService.listarPorProceso(empresaB, procesoB).getFirst().getId();
        Long poolProveedorB = poolService
                .crear(empresaB, procesoB, "Proveedor", TipoParticipante.PROVEEDOR, true).getId();
        Long rolB = rolProcesoService.crear(empresaB, "Comprador", "Gestiona las compras").getId();
        laneService.crear(empresaB, poolB, "Compras", rolB);
        mensajeService.crear(empresaB, procesoB, "Orden de compra", "Pedido", poolB, poolProveedorB);

        tokenA = login(ADMIN_A);
        tokenB = login(ADMIN_B);
    }

    Stream<Arguments> listadosPorRecursoPadre() {
        return Stream.of(
                Arguments.of("/api/procesos/{id}/pools", procesoB),
                Arguments.of("/api/procesos/{id}/mensajes", procesoB),
                Arguments.of("/api/pools/{id}/lanes", poolB));
    }

    @ParameterizedTest(name = "GET {0}")
    @MethodSource("listadosPorRecursoPadre")
    void Aislamiento_listarDesdeRecursoPadreDeOtraEmpresa_devuelve404(String ruta, Long idDeLaEmpresaB)
            throws Exception {
        // La empresa B si ve su listado: el 404 de la empresa A no se debe a una ruta o un id equivocados.
        mockMvc.perform(get(ruta, idDeLaEmpresaB).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty());

        mockMvc.perform(get(ruta, idDeLaEmpresaB).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    private String login(String email) throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest(email, CLAVE))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(respuesta).get("accessToken").asString();
    }
}
