package com.br.psyke.psyke.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {
        var token = extract(req);
        if (token != null && jwt.isValid(token)) {
            TenantContext.setTenantId(jwt.tenantId(token));
            TenantContext.setClinicId(jwt.clinicId(token));
            var auth = new UsernamePasswordAuthenticationToken(jwt.userId(token), null,
                jwt.roles(token).stream().map(SimpleGrantedAuthority::new).toList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        try { chain.doFilter(req, res); }
        finally { TenantContext.clear(); }
    }

    private String extract(HttpServletRequest req) {
        var b = req.getHeader("Authorization");
        if (StringUtils.hasText(b) && b.startsWith("Bearer ")) return b.substring(7);
        return null;
    }
}
