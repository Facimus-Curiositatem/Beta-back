package com.facimus.procesos.gestion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.facimus.procesos.security.ApiPrincipalRequestPostProcessor.principal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.config.ModelMapperConfig;
import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.gestion.controller.dto.LoginRequest;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;
import com.facimus.procesos.security.ApiPrincipal;
import com.facimus.procesos.security.JwtService;

import tools.jackson.databind.json.JsonMapper;


import java.util.Optional;

import com.facimus.procesos.gestion.controller.dto.RefreshRequest;

import io.jsonwebtoken.Claims;

@WebMvcTest(AuthController.class)
@Import(ModelMapperConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/v1/auth/login - credenciales validas devuelven el token (200)")
    void AuthController_login_credencialesValidas_devuelveToken() throws Exception {
        // Arrange
        Usuario usuario = usuario();
        given(usuarioService.autenticar("juan@acme.com", "secret123")).willReturn(usuario);
        given(jwtService.generarToken(any(ApiPrincipal.class))).willReturn("token-de-prueba");
        given(jwtService.getExpirationSeconds()).willReturn(1800L);
                given(jwtService.generarRefreshToken(any(ApiPrincipal.class))).willReturn("refresh-de-prueba");

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest("juan@acme.com", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-de-prueba"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.usuario.email").value("juan@acme.com"))
                .andExpect(jsonPath("$.usuario.rolAcceso").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-de-prueba"));

        then(jwtService).should()
                .generarToken(new ApiPrincipal(usuario.getId(), usuario.getEmpresa().getId(), RolAcceso.ADMINISTRADOR,
                        "juan@acme.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - credenciales invalidas devuelven 401 sin generar token")
    void AuthController_login_credencialesInvalidas_devuelve401() throws Exception {
        // Arrange
        given(usuarioService.autenticar("juan@acme.com", "mala"))
                .willThrow(new ReglaNegocioException("Correo o contrasena incorrectos."));

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest("juan@acme.com", "mala"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - campos vacios devuelven 400")
    void AuthController_login_camposVacios_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new LoginRequest("", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - usuario autenticado recibe 204")
    void AuthController_logout_autenticado_devuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(principal(RolAcceso.ADMINISTRADOR)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - sin autenticar devuelve 401")
    void AuthController_logout_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    private Usuario usuario() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Acme Corp");

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombre("Juan");
        usuario.setEmail("juan@acme.com");
        usuario.setRolAcceso(RolAcceso.ADMINISTRADOR);
        usuario.setActivo(true);
        usuario.setEmpresa(empresa);
        return usuario;
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - refresh token valido devuelve tokens nuevos (200)")
    void AuthController_refresh_refreshValido_devuelveTokensNuevos() throws Exception {
        // Arrange
        Usuario usuario = usuario();
        Claims claims = claimsDe(usuario);
        given(jwtService.validar("refresh-valido")).willReturn(Optional.of(claims));
        given(jwtService.esRefreshToken(claims)).willReturn(true);
        given(usuarioService.obtener(1L, 10L)).willReturn(usuario);
        given(jwtService.generarToken(any(ApiPrincipal.class))).willReturn("access-nuevo");
        given(jwtService.generarRefreshToken(any(ApiPrincipal.class))).willReturn("refresh-nuevo");

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new RefreshRequest("refresh-valido"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-nuevo"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-nuevo"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - un access token devuelve 401 sin consultar al usuario")
    void AuthController_refresh_accessToken_devuelve401() throws Exception {
        // Arrange
        Claims claims = claimsDe(usuario());
        given(jwtService.validar("access-token")).willReturn(Optional.of(claims));
        given(jwtService.esRefreshToken(claims)).willReturn(false);

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new RefreshRequest("access-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        then(usuarioService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - token invalido o expirado devuelve 401")
    void AuthController_refresh_tokenInvalidoOExpirado_devuelve401() throws Exception {
        // Arrange
        given(jwtService.validar("vencido")).willReturn(Optional.empty());

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new RefreshRequest("vencido"))))
                .andExpect(status().isUnauthorized());

        then(usuarioService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - sin refreshToken devuelve 400")
    void AuthController_refresh_sinCampo_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - usuario desactivado devuelve 401")
    void AuthController_refresh_usuarioDesactivado_devuelve401() throws Exception {
        // Arrange
        Usuario usuario = usuario();
        usuario.setActivo(false);
        Claims claims = claimsDe(usuario);
        given(jwtService.validar("refresh-de-desactivado")).willReturn(Optional.of(claims));
        given(jwtService.esRefreshToken(claims)).willReturn(true);
        given(usuarioService.obtener(1L, 10L)).willReturn(usuario);

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new RefreshRequest("refresh-de-desactivado"))))
                .andExpect(status().isUnauthorized());
    }

    /** Claims reales de un refresh token, generados con un JwtService de verdad (el del test es un mock). */
    private Claims claimsDe(Usuario usuario) {
        JwtService real = new JwtService("clave-de-pruebas-de-al-menos-32-bytes", 15, 7);
        return real.validar(real.generarRefreshToken(ApiPrincipal.of(usuario))).orElseThrow();
    }
}
