package com.br.psyke.psyke.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            MDC.put("requestId", UUID.randomUUID().toString().substring(0, 8));
            var token = extract(req);
            if (token != null) {
                jwt.parseAccessToken(token).ifPresent(at -> {
                    TenantContext.setTenantId(at.tenantId());
                    TenantContext.setClinicId(at.clinicId());
                    MDC.put("tenantId", at.tenantId());
                    MDC.put("userId", at.userId().toString());

                    var auth = new UsernamePasswordAuthenticationToken(
                            at.userId(), null,
                            at.roles().stream().map(SimpleGrantedAuthority::new).toList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            MDC.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String extract(HttpServletRequest req) {
        var b = req.getHeader("Authorization");
        if (StringUtils.hasText(b) && b.startsWith("Bearer ")) return b.substring(7);
        return null;
    }
}
