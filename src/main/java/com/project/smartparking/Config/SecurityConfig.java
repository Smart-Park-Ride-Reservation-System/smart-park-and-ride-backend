package com.project.smartparking.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/slots/generate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/slots/search").hasAnyRole("ADMIN", "COMMUTER")
                        .requestMatchers("/api/slots/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/reservations", "/api/reservations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservations", "/api/reservations/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/reservations", "/api/reservations/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations", "/api/reservations/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/payments/payhere/init").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/payhere/notify").permitAll()
                        .requestMatchers("/api/payments", "/api/payments/**").hasAnyRole("ADMIN", "ATTENDANT")

                        .requestMatchers(HttpMethod.POST, "/api/lots", "/api/lots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/lots", "/api/lots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/lots", "/api/lots/**").hasRole("ADMIN")
                        .requestMatchers("/api/lots/**").permitAll()

                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/hubs/**").permitAll()

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}