package com.br.psyke.psyke.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window, per-IP throttling for the endpoints attackers automate: credential stuffing on
 * login/refresh, password-reset flooding and mass tenant signup (OWASP API2/API4/API6).
 *
 * <p>State is in memory, which is enough for a single instance. Behind a proxy, enable
 * {@code server.forward-headers-strategy=framework} so {@code getRemoteAddr()} is the client IP.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String method, String path, int limit) {}

    private final List<Rule> rules;
    private final long windowMs;
    private final boolean enabled;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${psyke.rate-limit.enabled:true}") boolean enabled,
            @Value("${psyke.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${psyke.rate-limit.login-per-window:10}") int loginLimit,
            @Value("${psyke.rate-limit.sensitive-per-window:5}") int sensitiveLimit) {
        this.enabled = enabled;
        this.windowMs = windowSeconds * 1000;
        this.rules = List.of(
                new Rule("POST", "/auth/login", loginLimit),
                new Rule("POST", "/auth/refresh", loginLimit * 3),
                new Rule("POST", "/auth/forgot-password", sensitiveLimit),
                new Rule("POST", "/auth/reset-password", sensitiveLimit),
                new Rule("POST", "/auth/verify-email", sensitiveLimit),
                new Rule("POST", "/tenants", sensitiveLimit));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !enabled || match(request) == null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {
        var rule = match(req);
        var key = rule.path() + "|" + req.getRemoteAddr();
        var now = System.currentTimeMillis();
        var window = windows.compute(key, (k, w) -> w == null || now - w.start >= windowMs ? new Window(now) : w);

        if (window.count.incrementAndGet() > rule.limit()) {
            var retryAfter = Math.max(1, (window.start + windowMs - now) / 1000);
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retryAfter));
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Muitas tentativas. Aguarde " + retryAfter + " segundos e tente novamente.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    @Scheduled(fixedDelay = 300_000)
    void evictExpired() {
        var now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().start >= windowMs);
    }

    private Rule match(HttpServletRequest req) {
        var path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.startsWith("/api/")) path = path.substring(4);
        for (var r : rules) {
            if (r.method().equalsIgnoreCase(req.getMethod()) && r.path().equals(path)) return r;
        }
        return null;
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();
        Window(long start) { this.start = start; }
    }
}
