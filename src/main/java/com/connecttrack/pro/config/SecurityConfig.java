package com.connecttrack.pro.config;

import com.connecttrack.pro.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    // ---------------------------------------------------------
    // PASSWORD ENCODER
    // ---------------------------------------------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ---------------------------------------------------------
    // AUTHENTICATION MANAGER
    // ---------------------------------------------------------
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    // ---------------------------------------------------------
    // EXCLUDE STATIC USER FILES
    // ---------------------------------------------------------
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/user-uploads/**",
                "/api/v1/user-uploads/**"
        );
    }

    // ---------------------------------------------------------
    // SECURITY FILTER CHAIN
    // ---------------------------------------------------------
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // --------------------------------------------------
                // PUBLIC ENDPOINTS
                // --------------------------------------------------
                .requestMatchers(
                        "/",
                        "/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api/v1/auth/**",
                        "/ws/**"
                ).permitAll()

                // Public static images
                .requestMatchers(HttpMethod.GET, "/public/images/**").permitAll()

                // Existing public uploads
                .requestMatchers("/user-uploads/**").permitAll()
                .requestMatchers("/api/v1/user-uploads/**").permitAll()

                // --------------------------------------------------
                // AUTHENTICATED READ ACCESS
                // --------------------------------------------------
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/wifi-routers").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/settings/location").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/settings/timings").authenticated()

                // --------------------------------------------------
                // ADMIN ONLY (MODIFY SETTINGS)
                // --------------------------------------------------
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/**")
                        .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_SECTION_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/admin/**")
                        .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_SECTION_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/**")
                        .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_SECTION_ADMIN")

                // --------------------------------------------------
                // ANY OTHER ADMIN ENDPOINT
                // --------------------------------------------------
                .requestMatchers("/api/v1/admin/**")
                        .hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_SECTION_ADMIN")

                // --------------------------------------------------
                // DEFAULT RULE
                // --------------------------------------------------
                .anyRequest().authenticated()
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ---------------------------------------------------------
    // CORS CONFIG
    // ---------------------------------------------------------
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}