package com.jalaldeveloper.accountingsystem.platform.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PlatformSecurityProperties.class)
public class PlatformSecurityConfiguration {

    @Bean
    PasswordEncoder platformPasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    CorsConfigurationSource platformCorsConfigurationSource(
            @Value("${accounting.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split("\\s*,\\s*")).map(String::trim).toList();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    @Bean
    SecurityFilterChain platformSecurityFilterChain(
            HttpSecurity http,
            PlatformSecurityProperties securityProperties,
            ObjectProvider<JwtAuthenticationFilter> jwtFilter,
            CorsConfigurationSource platformCorsConfigurationSource) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(c -> c.configurationSource(platformCorsConfigurationSource));

        if (!securityProperties.isEnabled()) {
            return http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
        }

        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        http.authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/security-config", "/api/v1/auth/me").permitAll()
                .requestMatchers("/h2-console", "/h2-console/**").permitAll()
                .requestMatchers("/api/**").authenticated());
        http.exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}");
                }));
        jwtFilter.ifAvailable(f -> http.addFilterBefore(f, UsernamePasswordAuthenticationFilter.class));
        return http.build();
    }
}
