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
                                                // Public / Auth / Gate Scanners
                                                .requestMatchers("/", "/gate", "/gate/**", "/gate/*").permitAll()
                                                .requestMatchers("/api/reservations/gate/**", "/api/reservations/gate/*").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/home").permitAll()

                                                // Payments (Webhooks/Init)
                                                .requestMatchers(HttpMethod.GET, "/api/payments/payhere/init")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/payments/payhere/notify")
                                                .permitAll()
                                                .requestMatchers("/api/payments/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN", "ATTENDANT")

                                                .requestMatchers(HttpMethod.GET, "/api/subscriptions/packages",
                                                                "/api/subscriptions/packages/**")
                                                .permitAll()
                                                .requestMatchers("/api/subscriptions/**")
                                                .hasAnyRole("COMMUTER", "ADMIN", "HUB_ADMIN", "ATTENDANT")

                                                // Slots Security
                                                .requestMatchers(HttpMethod.GET, "/api/slots/recommendation")
                                                .hasAnyRole("COMMUTER", "ADMIN", "HUB_ADMIN", "ATTENDANT")
                                                .requestMatchers(HttpMethod.GET, "/api/slots/search/**", "/api/v1/parking/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/slots/stream", "/api/slots/stream/",
                                                                "/api/sensors/activity/stream", "/api/sensors/activity/stream/")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/slots", "/api/slots/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN", "ATTENDANT")
                                                .requestMatchers(HttpMethod.POST, "/api/slots/generate")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/slots/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN", "ATTENDANT")
                                                .requestMatchers(HttpMethod.PUT, "/api/slots/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN", "ATTENDANT")
                                                .requestMatchers(HttpMethod.DELETE, "/api/slots/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN")

                                                // Reservations
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/gate-*/scan")
                                                .hasAnyRole("ATTENDANT", "HUB_ADMIN", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/checkin",
                                                                "/api/reservations/*/checkin")
                                                .hasAnyRole("ATTENDANT", "HUB_ADMIN", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/*/checkout")
                                                .hasAnyRole("ATTENDANT", "HUB_ADMIN", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/reservations/*/paid")
                                                .hasAnyRole("ATTENDANT", "HUB_ADMIN", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/reservations/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/reservations", "/api/reservations/**")
                                                .hasAnyRole("COMMUTER", "ATTENDANT", "HUB_ADMIN", "ADMIN")

                                                // Lots & Hubs
                                                .requestMatchers(HttpMethod.GET, "/api/hubs/**", "/api/lots/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/hubs/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/hubs/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/hubs/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/lots/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/lots/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/lots/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN")

                                                // Users & AI
                                                // before the broader /api/users/** rules
                                                .requestMatchers("/api/users/me").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/users/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN")
                                                .requestMatchers("/api/users/**", "/api/ai-insights").hasRole("ADMIN")

                                                // Notifications — commuters can read their own notifications
                                                .requestMatchers(HttpMethod.GET, "/api/user-notifications/**")
                                                .hasAnyRole("COMMUTER", "ATTENDANT", "HUB_ADMIN", "ADMIN")
                                                .requestMatchers("/api/user-notifications/**")
                                                .hasAnyRole("COMMUTER", "ATTENDANT", "HUB_ADMIN", "ADMIN")

                                                .requestMatchers("/api/notifications/broadcasts/**")
                                                .hasAnyRole("ADMIN", "HUB_ADMIN")

                                                .requestMatchers("/api/admin/**")
                                                .hasRole("ADMIN")
                                                
                                                // Sensors
                                                .requestMatchers("/api/sensors", "/api/sensors/**")
                                                .hasRole("ADMIN")

                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}