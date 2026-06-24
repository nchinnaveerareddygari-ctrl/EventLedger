package com.eventledger.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Basic OAuth2 Resource Server Security Configuration
 * Uses OAuth2 provider tokens (JWT) for authentication
 * No custom user mapping - uses standard OAuth2 attributes
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class OAuth2ResourceServerConfig {

    /**
     * Password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT Authentication Converter
     * Converts JWT token claims to Spring Security authorities
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Extract roles from "roles" claim in JWT
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = jwt.getClaimAsStringList("roles");
            if (authorities == null) {
                authorities = java.util.Collections.emptyList();
            }
            return authorities.stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                    .collect(java.util.stream.Collectors.toList());
        });

        return converter;
    }

    /**
     * Configure Security Filter Chain
     * Basic OAuth2 resource server configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        log.info("Configuring basic OAuth2 Resource Server Security");

        http
                // CORS Configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF for stateless API
                .csrf(csrf -> csrf.disable())

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Exception handling
                .exceptionHandling(ex -> {
                    ex.authenticationEntryPoint((request, response, authException) -> {
                        log.warn("Unauthorized access: {}", authException.getMessage());
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"status\": 401, \"message\": \"Unauthorized\", \"error\": \"" + authException.getMessage() + "\"}"
                        );
                    });
                })

                // Authorization configuration
                .authorizeHttpRequests(authz -> {
                    // Public endpoints
                    authz.requestMatchers("/health").permitAll()
                            .requestMatchers("/swagger-ui.html").permitAll()
                            .requestMatchers("/swagger-ui/**").permitAll()
                            .requestMatchers("/v3/api-docs").permitAll()
                            .requestMatchers("/v3/api-docs/**").permitAll()
                            .requestMatchers("/swagger-resources").permitAll()
                            .requestMatchers("/swagger-resources/**").permitAll()
                            .requestMatchers("/webjars/**").permitAll()
                            .requestMatchers("/h2-console/**").permitAll()

                            // Protected endpoints - require OAuth2 token
                            .requestMatchers(HttpMethod.POST, "/events").authenticated()
                            .requestMatchers(HttpMethod.GET, "/events").authenticated()
                            .requestMatchers(HttpMethod.GET, "/events/**").authenticated()

                            // All other requests require authentication
                            .anyRequest().authenticated()
                })

                // OAuth2 Resource Server configuration
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(jwt -> {
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                    });
                });

        return http.build();
    }
}
