package com.facimus.procesos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security (con su cadena de filtros y @PreAuthorize) llega en la
 * Entrega 3. Por ahora solo se usa spring-security-crypto para no
 * almacenar contrasenas en texto plano (HU-03).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
