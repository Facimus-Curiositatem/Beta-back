package com.facimus.procesos.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad para los @WebMvcTest, donde SecurityConfig no se carga: mismas reglas de acceso y mismas
 * respuestas 401/403 que en produccion, pero la identidad sale de la sesion simulada del test.
 * Con la aplicacion completa (SecurityConfig presente) no se activa.
 */
@AutoConfiguration(before = ServletWebSecurityAutoConfiguration.class)
@EnableWebSecurity
public class SeguridadControllersTestAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain seguridadControllersTest(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        SecurityConfig.reglasComunes(http)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new JwtAuthEntryPoint(jsonMapper))
                        .accessDeniedHandler(new JwtAccessDeniedHandler(jsonMapper)))
                .addFilterBefore(new SesionDePruebaFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
