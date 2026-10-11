package com.br.psyke.psyke.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-account brute-force protection (OWASP API2/API6). {@link RateLimitFilter} throttles by client IP,
 * which an attacker can dodge by rotating {@code X-Forwarded-For} behind some proxies; this limits
 * <em>failed</em> logins per clinic + e-mail regardless of where they come from.
 *
 * <p>Only failures count and a success clears the counter, so normal users are unaffected. The trade-off is
 * that someone can temporarily block a known account by failing on purpose; the block ends with the window.
 * State is in memory (single instance).
 */
@Component
public class LoginAttemptLimiter {

    private static final int MAX_TRACKED_ACCOUNTS = 50_000;

    private record Entry(long windowStart, int failures) {}

    private final Map<String, Entry> failures = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final long windowMs;

    public LoginAttemptLimiter(
            @Value("${psyke.rate-limit.account-max-failures:10}") int maxFailures,
            @Value("${psyke.rate-limit.account-window-seconds:900}") long windowSeconds) {
        this.maxFailures = maxFailures;
        this.windowMs = windowSeconds * 1000;
    }

    /** Seconds the account must wait, or 0 when another attempt is allowed. */
    public long retryAfterSeconds(String tenantId, String email) {
        var e = failures.get(key(tenantId, email));
        if (e == null) return 0;
        var remaining = e.windowStart() + windowMs - System.currentTimeMillis();
        if (remaining <= 0 || e.failures() < maxFailures) return 0;
        return Math.max(1, remaining / 1000);
    }

    public void recordFailure(String tenantId, String email) {
        if (failures.size() >= MAX_TRACKED_ACCOUNTS) evictExpired();
        if (failures.size() >= MAX_TRACKED_ACCOUNTS) return; // under flood: stay bounded, the IP limiter still applies
        var now = System.currentTimeMillis();
        failures.compute(key(tenantId, email), (k, e) ->
                e == null || now - e.windowStart() >= windowMs ? new Entry(now, 1) : new Entry(e.windowStart(), e.failures() + 1));
    }

    public void recordSuccess(String tenantId, String email) {
        failures.remove(key(tenantId, email));
    }

    @Scheduled(fixedDelay = 300_000)
    void evictExpired() {
        var now = System.currentTimeMillis();
        failures.entrySet().removeIf(e -> now - e.getValue().windowStart() >= windowMs);
    }

    private static String key(String tenantId, String email) {
        return tenantId.trim().toLowerCase(Locale.ROOT) + "|" + email.trim().toLowerCase(Locale.ROOT);
    }
}
