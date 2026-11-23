package com.facimus.procesos.gestion.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.common.ReglaNegocioException;
import org.modelmapper.ModelMapper;

import com.facimus.procesos.gestion.controller.dto.LoginRequest;
import com.facimus.procesos.gestion.controller.dto.LoginResponse;
import com.facimus.procesos.gestion.controller.dto.RefreshRequest;
import com.facimus.procesos.gestion.controller.dto.UsuarioResponse;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.UsuarioService;
import com.facimus.procesos.security.ApiPrincipal;
import com.facimus.procesos.security.JwtService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;

/** HU-03: inicio y cierre de sesion con JWT, y renovacion de tokens con refresh token. */
@Tag(name = "Autenticacion", description = "Inicio y cierre de sesion (HU-03).")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String TIPO_TOKEN = "Bearer";
    private static final String REFRESH_INVALIDO = "Refresh token invalido o expirado.";

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;

    public AuthController(UsuarioService usuarioService, JwtService jwtService, ModelMapper modelMapper) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.modelMapper = modelMapper;
    }

    @SecurityRequirements()
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Valida credenciales y devuelve un access token y un refresh token.")
    @ApiResponse(responseCode = "200", description = "Operacion exitosa.",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "400", description = "Solicitud invalida (errores de validacion o JSON malformado).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "No autenticado: token ausente o invalido.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        Usuario usuario;
        try {
            usuario = usuarioService.autenticar(request.email(), request.password());
        } catch (ReglaNegocioException e) {
            // Sin la causa a proposito: Spring MVC buscaria un handler para ella y responderia 409.
            // Asi lo resuelve JwtAuthEntryPoint con 401 y el mismo mensaje generico, exista o no el correo (HU-03).
            throw new BadCredentialsException(e.getMessage());
        }
        return ResponseEntity.ok(emitirTokens(usuario));
    }

    @SecurityRequirements()
    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens",
            description = "Recibe un refresh token vigente y devuelve tokens nuevos con el rol actual del usuario.")
    @ApiResponse(responseCode = "200", description = "Operacion exitosa.",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "400", description = "Solicitud invalida: falta el refreshToken.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Refresh token invalido, expirado o se envio un access token.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<LoginResponse> refrescar(@Validated @RequestBody RefreshRequest request) {
        Optional<Claims> claims = jwtService.validar(request.refreshToken());
        if (claims.isEmpty() || !jwtService.esRefreshToken(claims.get())) {
            throw new BadCredentialsException(REFRESH_INVALIDO);
        }
        Long empresaId = claims.get().get(JwtService.CLAIM_EMPRESA_ID, Long.class);
        Long usuarioId = claims.get().get(JwtService.CLAIM_USUARIO_ID, Long.class);

        // Se recarga de la BD: un cambio de rol o una desactivacion se reflejan en los tokens nuevos
        Usuario usuario;
        try {
            usuario = usuarioService.obtener(empresaId, usuarioId);
        } catch (RecursoNoEncontradoException e) {
            throw new BadCredentialsException(REFRESH_INVALIDO);
        }
        if (!usuario.isActivo()) {
            throw new BadCredentialsException(REFRESH_INVALIDO);
        }
        return ResponseEntity.ok(emitirTokens(usuario));
    }

    /** Sin estado en el servidor: cerrar sesion es que el cliente descarte su token. */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion", description = "Operacion stateless: no invalida el token, el cliente debe descartarlo.")
    @ApiResponse(responseCode = "204", description = "Operacion exitosa, sin contenido.")
    @ApiResponse(responseCode = "401", description = "No autenticado: token ausente o invalido.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private LoginResponse emitirTokens(Usuario usuario) {
        ApiPrincipal principal = ApiPrincipal.of(usuario);
        return new LoginResponse(jwtService.generarToken(principal), jwtService.generarRefreshToken(principal),
                TIPO_TOKEN, jwtService.getExpirationSeconds(), modelMapper.map(usuario, UsuarioResponse.class));
    }
}