package com.project.smartparking.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Value("${cors.allowed.origins}")
        private String allowedOrigins;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(
                                List.of(allowedOrigins.split(",")).stream().map(String::trim).toList());
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
                                                                .policyDirectives(
                                                                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                                                .referrerPolicy(referrer -> referrer
                                                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                .frameOptions(frame -> frame.deny())
                                                .permissionsPolicy(permissions -> permissions
                                                                .policy("camera=(self), microphone=(), geolocation=()")))
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(new HttpStatusEntryPoint(
                                                                HttpStatus.UNAUTHORIZED)))
                                .authorizeHttpRequests(auth -> auth
                                                // Public / Auth
                                                .requestMatchers("/").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/home").permitAll()

                                                // Payments (Webhooks/Init)
                                                .requestMatchers(HttpMethod.GET, "/api/payments/payhere/init")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/payments/payhere/notify")
                                                .permitAll()
                                                .requestMatchers("/api/payments/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN", "ROLE_ATTENDANT")

                                                // Slots (Public GETs)
                                                .requestMatchers(HttpMethod.GET, "/api/slots", "/api/slots/**",
                                                                "/api/slots/search",
                                                                "/api/slots/search/**", "/api/v1/parking/",
                                                                "/api/v1/parking/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/slots/generate")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/slots", "/api/slots/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN", "ROLE_ATTENDANT")
                                                .requestMatchers(HttpMethod.PUT, "/api/slots/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN", "ROLE_ATTENDANT")
                                                .requestMatchers(HttpMethod.DELETE, "/api/slots/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN")

                                                // Reservations
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/gate-*/scan")
                                                .hasAnyAuthority("ROLE_ATTENDANT", "ROLE_HUB_ADMIN", "ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/checkin",
                                                                "/api/reservations/*/checkin")
                                                .hasAnyAuthority("ROLE_ATTENDANT", "ROLE_HUB_ADMIN", "ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/*/checkout")
                                                .hasAnyAuthority("ROLE_ATTENDANT", "ROLE_HUB_ADMIN", "ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/*/paid")
                                                .hasAnyAuthority("ROLE_ATTENDANT", "ROLE_HUB_ADMIN", "ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/reservations/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers("/api/reservations/**")
                                                .hasAnyAuthority("ROLE_COMMUTER", "ROLE_ATTENDANT", "ROLE_HUB_ADMIN",
                                                                "ROLE_ADMIN")

                                                // Lots & Hubs (Public GETs)
                                                .requestMatchers(HttpMethod.GET, "/api/hubs", "/api/hubs/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/lots", "/api/lots/**",
                                                                "/api/lots/by-hub/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.POST, "/api/hubs/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/hubs/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/hubs/**")
                                                .hasAuthority("ROLE_ADMIN")

                                                .requestMatchers(HttpMethod.POST, "/api/lots/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/lots/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/lots/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HUB_ADMIN")

                                                // Users & AI
                                                .requestMatchers("/api/users/**", "/api/ai-insights")
                                                .hasAuthority("ROLE_ADMIN")

                                                // Subscriptions
                                                .requestMatchers(HttpMethod.GET, "/api/subscriptions/**").permitAll()
                                                .requestMatchers("/api/subscriptions/**")
                                                .hasAnyAuthority("ROLE_COMMUTER", "ROLE_ADMIN")

                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}