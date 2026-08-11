package com.project.smartparking.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {
                        })
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()")))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // ---- Slots ----
                        .requestMatchers(HttpMethod.POST, "/api/slots/generate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/slots/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/slots/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/slots/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers(HttpMethod.PUT, "/api/slots/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers(HttpMethod.DELETE, "/api/slots/**").hasRole("ADMIN")

                        // ---- Reservations ----
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/gate-a/scan")
                        .hasAnyRole("ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/gate-b/scan")
                        .hasAnyRole("ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/checkin").hasAnyRole("ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/checkin").hasAnyRole("ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/checkout")
                        .hasAnyRole("ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/paid").hasAnyRole("ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/confirm-payment")
                        .hasAnyRole("ATTENDANT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reservations").hasAnyRole("ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasAnyRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reservations/user/**")
                        .hasAnyRole("COMMUTER", "ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/**")
                        .hasAnyRole("COMMUTER", "ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations")
                        .hasAnyRole("COMMUTER", "ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/cancel")
                        .hasAnyRole("COMMUTER", "ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/**")
                        .hasAnyRole("COMMUTER", "ATTENDANT", "ADMIN")

                        // ---- Payments ----
                        .requestMatchers(HttpMethod.GET, "/api/payments/payhere/init").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/payhere/notify").permitAll()
                        .requestMatchers("/api/payments", "/api/payments/**").hasAnyRole("ADMIN", "ATTENDANT")

                        // ---- Lots ----
                        .requestMatchers(HttpMethod.GET, "/api/lots", "/api/lots/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/lots", "/api/lots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/lots", "/api/lots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/lots", "/api/lots/**").hasRole("ADMIN")

                        // ---- Users ----
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // ---- Hubs ----
                        .requestMatchers(HttpMethod.GET, "/api/hubs/**").hasAnyRole("COMMUTER", "ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/hubs/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers(HttpMethod.PUT, "/api/hubs/**").hasAnyRole("ADMIN", "ATTENDANT")
                        .requestMatchers(HttpMethod.DELETE, "/api/hubs/**").hasRole("ADMIN")

                        .requestMatchers("/api/ai-insights").hasRole("ADMIN")

                        // ---- Subscriptions ----
                        .requestMatchers(HttpMethod.GET, "/api/subscriptions/**")
                        .hasAnyRole("COMMUTER", "ATTENDANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/subscriptions/**").hasAnyRole("COMMUTER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/subscriptions/**").hasAnyRole("COMMUTER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/subscriptions/**").hasAnyRole("COMMUTER", "ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}