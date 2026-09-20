package com.facimus.procesos.security;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.service.UsuarioService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMINISTRADOR = RolAcceso.ADMINISTRADOR.name();
    private static final String EDITOR = RolAcceso.EDITOR.name();

    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthEntryPoint jwtAuthEntryPoint, JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UsuarioService usuarioService)
            throws Exception {
        reglasComunes(http)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                // Sin @Bean a proposito: como bean, Spring Boot tambien lo registraria como filtro del servlet.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarioService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Reglas compartidas con la configuracion de seguridad de los tests de controllers. */
    static HttpSecurity reglasComunes(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/empresas").permitAll()
                        .requestMatchers("/h2-console/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/error").permitAll()
                        // Matriz de permisos de las HU: cualquier rol consulta, administrador y editor modifican,
                        // y el administrador se reserva usuarios (HU-02), roles (HU-17 a HU-19) y los borrados
                        // de procesos (HU-06), actividades (HU-10), arcos (HU-13) y gateways (HU-16).
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(rutas("/usuarios/**")).hasAuthority(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(rutas("/roles/**")).hasAuthority(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.DELETE, rutas("/procesos/**", "/actividades/**", "/arcos/**",
                                "/gateways/**")).hasAuthority(ADMINISTRADOR)
                        .requestMatchers("/api/**").hasAnyAuthority(ADMINISTRADOR, EDITOR)
                        .anyRequest().authenticated());
    }

    // ponytail: cada recurso con y sin /v1 mientras Persona 2 migra las rutas; al terminar queda solo /api/v1.
    private static String[] rutas(String... recursos) {
        return Arrays.stream(recursos)
                .flatMap(recurso -> Stream.of("/api" + recurso, "/api/v1" + recurso))
                .toArray(String[]::new);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
