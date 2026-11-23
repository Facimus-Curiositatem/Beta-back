package com.facimus.procesos.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.facimus.procesos.gestion.model.RolAcceso;

import io.jsonwebtoken.Claims;

import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-de-al-menos-32-bytes";

    private final ApiPrincipal editor = new ApiPrincipal(10L, 1L, RolAcceso.EDITOR, "ana@acme.com");

    @Test
    @DisplayName("Un token recien generado es valido y trae los datos del usuario")
    void JwtService_validar_tokenGenerado_devuelveClaimsDelPrincipal() {
        JwtService jwtService = new JwtService(SECRETO, 15, 7);

        Optional<Claims> claims = jwtService.validar(jwtService.generarToken(editor));

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("ana@acme.com");
        assertThat(claims.get().get(JwtService.CLAIM_USUARIO_ID, Long.class)).isEqualTo(10L);
        assertThat(claims.get().get(JwtService.CLAIM_EMPRESA_ID, Long.class)).isEqualTo(1L);
        assertThat(claims.get().get(JwtService.CLAIM_ROL, String.class)).isEqualTo("EDITOR");
    }

    @Test
    @DisplayName("Cambiar la empresa dentro del token invalida la firma")
    void JwtService_validar_payloadDeOtraEmpresa_devuelveVacio() {
        JwtService jwtService = new JwtService(SECRETO, 15, 7);
        String[] original = jwtService.generarToken(editor).split("\\.");
        String[] otraEmpresa = jwtService.generarToken(new ApiPrincipal(10L, 2L, RolAcceso.EDITOR, "ana@acme.com"))
                .split("\\.");

        String manipulado = original[0] + "." + otraEmpresa[1] + "." + original[2];

        assertThat(jwtService.validar(manipulado)).isEmpty();
    }

    @Test
    @DisplayName("Un token expirado se rechaza")
    void JwtService_validar_tokenExpirado_devuelveVacio() {
        JwtService jwtService = new JwtService(SECRETO, -1, 7);

        assertThat(jwtService.validar(jwtService.generarToken(editor))).isEmpty();
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza")
    void JwtService_validar_firmadoConOtraClave_devuelveVacio() {
        JwtService emisor = new JwtService("otra-clave-distinta-de-al-menos-32-bytes", 15, 7);
        JwtService receptor = new JwtService(SECRETO, 15, 7);

        assertThat(receptor.validar(emisor.generarToken(editor))).isEmpty();
    }

    @Test
    @DisplayName("Un texto que no es JWT se rechaza sin lanzar excepcion")
    void JwtService_validar_textoCualquiera_devuelveVacio() {
        JwtService jwtService = new JwtService(SECRETO, 15, 7);

        assertThat(jwtService.validar("no-es-un-token")).isEmpty();
    }

    @Test
    @DisplayName("Sin JWT_SECRET usa una clave aleatoria y los tokens siguen funcionando")
    void JwtService_generarToken_sinSecreto_usaClaveAleatoria() {
        JwtService jwtService = new JwtService("", 15, 7);

        assertThat(jwtService.validar(jwtService.generarToken(editor))).isPresent();
    }
    
    @Test
    @DisplayName("El access token dura los minutos configurados, trae emisor y rol, y no es refresh")
    void JwtService_generarToken_traeEmisorRolYDuraMinutos() {
        JwtService jwtService = new JwtService(SECRETO, 15, 7);

        Claims claims = jwtService.validar(jwtService.generarToken(editor)).orElseThrow();

        assertThat(claims.getIssuer()).isEqualTo("proyecto-procesos");
        assertThat(claims.get(JwtService.CLAIM_ROL, String.class)).isEqualTo("EDITOR");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(15 * 60 * 1000L);
        assertThat(jwtService.esRefreshToken(claims)).isFalse();
    }

    @Test
    @DisplayName("El refresh token dura los dias configurados, tiene tipo refresh y no trae rol")
    void JwtService_generarRefreshToken_tipoRefreshSinRolYDuraDias() {
        JwtService jwtService = new JwtService(SECRETO, 15, 7);

        Claims claims = jwtService.validar(jwtService.generarRefreshToken(editor)).orElseThrow();

        assertThat(jwtService.esRefreshToken(claims)).isTrue();
        assertThat(claims.get("tipo", String.class)).isEqualTo("refresh");
        assertThat(claims.get(JwtService.CLAIM_ROL)).isNull();
        assertThat(claims.getSubject()).isEqualTo("ana@acme.com");
        assertThat(claims.getIssuer()).isEqualTo("proyecto-procesos");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(7L * 24 * 60 * 60 * 1000);
    }

    @Test
    @DisplayName("Un refresh token expirado se rechaza")
    void JwtService_validar_refreshExpirado_devuelveVacio() {
        JwtService jwtService = new JwtService(SECRETO, 15, -1);

        assertThat(jwtService.validar(jwtService.generarRefreshToken(editor))).isEmpty();
    }

    @Test
    @DisplayName("Un token con la clave correcta pero sin nuestro emisor se rechaza")
    void JwtService_validar_otroEmisor_devuelveVacio() {
        JwtService jwtService = new JwtService(SECRETO, 15, 7);
        String ajeno = Jwts.builder()
                .subject("ana@acme.com")
                .issuer("otro-sistema")
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThat(jwtService.validar(ajeno)).isEmpty();
    }
}
