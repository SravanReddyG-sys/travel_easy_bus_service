package com.traveleasy.bus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveleasy.bus.dto.response.ErrorResponse;
import com.traveleasy.bus.exception.ErrorCode;
import com.traveleasy.bus.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.security.cors.allowed-origins:http://localhost:4200,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Health, Actuator & Swagger Endpoints
                        .requestMatchers("/api/v1/health/**", "/api/v1/auth/**", "/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                        // Public Traveller Read & Search APIs
                        .requestMatchers(HttpMethod.GET, "/api/v1/buses/**").permitAll()
                        .requestMatchers("/api/v1/routes/**").permitAll()
                        .requestMatchers("/api/v1/schedules/**").permitAll()

                        // Seat lock, confirm, and release workflows (Traveller / Booking integration)
                        .requestMatchers(HttpMethod.POST, "/api/v1/schedules/*/seats/lock").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/schedules/*/seats/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/schedules/*/seats/release").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/schedules/*/seats/cleanup-expired-locks").permitAll()
                        .requestMatchers("/api/v1/integration/booking/**").permitAll()
                        .requestMatchers("/api/v1/notifications/**").permitAll()

                        // Operator Specific Endpoints
                        .requestMatchers("/api/v1/operator/**").hasAnyRole("OPERATOR", "ADMIN")

                        // All other APIs require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("http://127.0.0.1:*");
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .code(ErrorCode.UNAUTHORIZED.getCode())
                    .message("Authentication token is missing or invalid: " + authException.getMessage())
                    .path(request.getRequestURI())
                    .build();
            response.getOutputStream().write(objectMapper.writeValueAsBytes(errorResponse));
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.FORBIDDEN.value())
                    .code(ErrorCode.FORBIDDEN.getCode())
                    .message("Access forbidden: Insufficient permissions to access this resource")
                    .path(request.getRequestURI())
                    .build();
            response.getOutputStream().write(objectMapper.writeValueAsBytes(errorResponse));
        };
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
