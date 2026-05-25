package com.pkfrc.rdv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration Spring Security 6.x.
 * API stateless avec Basic Auth (JWT OAuth2 à ajouter en production).
 * Utilise SecurityFilterChain lambda DSL — plus de WebSecurityConfigurerAdapter.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)  // API stateless : CSRF inutile
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger & actuator : accès libre
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // Prometheus : authentifié
                        .requestMatchers("/actuator/**").authenticated()
                        // GET sur le référentiel : public
                        .requestMatchers(HttpMethod.GET, "/api/v1/referentiel/**").permitAll()
                        // Tout le reste : authentifié
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
