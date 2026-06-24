package com.eventledger.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security Configuration for Account Service
 *
 * This configuration:
 * - Enables HTTP Basic and Form-based authentication
 * - Configures role-based access control (RBAC)
 * - Implements CORS for cross-origin requests
 * - Uses stateless session management
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    /**
     * Configure HTTP security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS configuration
                .cors()
                .and()
                // CSRF disabled for stateless APIs (can be enabled if needed)
                .csrf()
                .disable()
                // HTTP Basic authentication for API calls
                .httpBasic()
                .and()
                // Stateless session management
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // Authorization configuration
                .authorizeRequests()
                // Public endpoints - no authentication required
                .antMatchers("/api/v1/health").permitAll()
                .antMatchers("/swagger-ui/**").permitAll()
                .antMatchers("/v3/api-docs/**").permitAll()
                .antMatchers("/swagger-ui.html").permitAll()
                .antMatchers("/webjars/**").permitAll()
                .antMatchers("/favicon.ico").permitAll()

                // Actuator endpoints - require ADMIN role
                .antMatchers("/actuator/**").hasRole("ADMIN")

                // Read operations - require READ or ADMIN role
                .antMatchers(HttpMethod.GET, "/api/v1/accounts/**").hasAnyRole("USER", "ADMIN")

                // Write operations - require WRITE or ADMIN role
                .antMatchers(HttpMethod.POST, "/api/v1/accounts/**").hasAnyRole("WRITE", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/v1/accounts/**").hasAnyRole("WRITE", "ADMIN")
                .antMatchers(HttpMethod.PATCH, "/api/v1/accounts/**").hasAnyRole("WRITE", "ADMIN")

                // Delete operations - require ADMIN role only
                .antMatchers(HttpMethod.DELETE, "/api/v1/accounts/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
                .and()
                // Exception handling
                .exceptionHandling()
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new CustomAccessDeniedHandler());

        return http.build();
    }

    /**
     * Password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS Configuration
     * Allows cross-origin requests from trusted sources
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins - configure based on environment
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:4200",
                "https://yourdomain.com"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Trace-ID",
                "X-Request-ID"
        ));

        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
                "X-Trace-ID",
                "X-Request-ID",
                "Content-Type"
        ));

        // Allow credentials (Basic Auth)
        configuration.setAllowCredentials(true);

        // Max age for preflight requests (in seconds)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}