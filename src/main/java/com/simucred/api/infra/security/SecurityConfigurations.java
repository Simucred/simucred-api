package com.simucred.api.infra.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    try {
      return http
          .cors(Customizer.withDefaults())
          .csrf(csrf -> csrf.disable()) // CSRF desabilitado com segurança: API stateless via JWT
          .authorizeHttpRequests(req -> {
            req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v1/swagger").permitAll();
            req.anyRequest().authenticated();
          })
          .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
          .build();
    } catch (Exception e) {
      throw new SecurityConfigurationException("Erro ao configurar a cadeia de filtros de segurança", e);
    }
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:4200"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}