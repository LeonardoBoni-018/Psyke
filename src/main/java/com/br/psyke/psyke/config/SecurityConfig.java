package com.br.psyke.psyke.config;

import com.br.psyke.psyke.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] STAFF = {"ADMIN", "PROFESSIONAL", "RECEPTIONIST"};

    private final JwtAuthenticationFilter filter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable()) // stateless bearer-token API: no cookies, no CSRF surface
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .contentTypeOptions(c -> {})
                .frameOptions(f -> f.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()")))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/forgot-password",
                                 "/auth/reset-password", "/auth/verify-email", "/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/tenants").permitAll()
                .requestMatchers("/confirm/**", "/cancel/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Patients only see their own data through the portal.
                .requestMatchers("/portal/**").hasAnyRole("PATIENT", "ADMIN")
                .requestMatchers("/auth/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/tenants/current").authenticated()
                .requestMatchers("/audit-logs/**").hasRole("ADMIN")
                // Everything else is the clinic back-office: staff roles only (deny-by-default for patients).
                .anyRequest().hasAnyRole(STAFF))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    writeError(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Token expirado ou inválido"))
                .accessDeniedHandler((req, res, ex) ->
                    writeError(req, res, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Acesso negado")))
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        var origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        // Patterns allow preview deployments such as https://psyke-web-*.vercel.app
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID", "X-Requested-With"));
        config.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
        config.setAllowCredentials(false);
        config.setMaxAge(86_400L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void writeError(HttpServletRequest req, HttpServletResponse res, int status,
                                   String error, String message) throws IOException {
        res.setContentType("application/json;charset=UTF-8");
        res.setStatus(status);
        var path = req.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\""
                + message + "\",\"path\":\"" + path + "\"}");
    }
}
