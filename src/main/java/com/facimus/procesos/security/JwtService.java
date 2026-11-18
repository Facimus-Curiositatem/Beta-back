package com.facimus.procesos.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    static final String CLAIM_USUARIO_ID = "usuarioId";
    static final String CLAIM_EMPRESA_ID = "empresaId";
    static final String CLAIM_ROL = "rol";

    private static final String EMISOR = "proyecto-procesos";
    private static final String CLAIM_TIPO = "tipo";
    private static final String TIPO_REFRESH = "refresh";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration expiracion;
    private final Duration expiracionRefresh;

    public JwtService(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long minutos,
            @Value("${jwt.refresh-expiration-days}") long diasRefresh) {
        if (secret.isBlank()) {
            log.warn("JWT_SECRET no definido: se usa una clave aleatoria y los tokens se invalidan al reiniciar.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expiracion = Duration.ofMinutes(minutos);
        this.expiracionRefresh = Duration.ofDays(diasRefresh);
    }

    /** Access token va acá */
    public String generarToken(ApiPrincipal principal) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(principal.email())
                .claim(CLAIM_USUARIO_ID, principal.usuarioId())
                .claim(CLAIM_EMPRESA_ID, principal.empresaId())
                .claim(CLAIM_ROL, principal.rol().name())
                .issuer(EMISOR)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**Refresh token va aca*/
    public String generarRefreshToken(ApiPrincipal principal) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(principal.email())
                .claim(CLAIM_USUARIO_ID, principal.usuarioId())
                .claim(CLAIM_EMPRESA_ID, principal.empresaId())
                .claim(CLAIM_TIPO, TIPO_REFRESH)
                .issuer(EMISOR)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionRefresh)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Claims del token si la firma, el emisor y la expiracion son validos y vacio en cualquier otro caso. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(EMISOR)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean esRefreshToken(Claims claims) {
        return TIPO_REFRESH.equals(claims.get(CLAIM_TIPO, String.class));
    }

    public long getExpirationSeconds() {
        return expiracion.toSeconds();
    }
}