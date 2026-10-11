package com.br.psyke.psyke;

import com.br.psyke.psyke.security.LoginAttemptLimiter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    @Test
    void blocksAccountAfterMaxFailuresRegardlessOfCase() {
        var limiter = new LoginAttemptLimiter(3, 900);
        for (int i = 0; i < 3; i++) {
            assertEquals(0, limiter.retryAfterSeconds("clinica", "ana@x.com"));
            limiter.recordFailure("Clinica", " ANA@x.com ");
        }
        assertTrue(limiter.retryAfterSeconds("clinica", "ana@x.com") > 0);
    }

    @Test
    void otherAccountsAndClinicsAreNotAffected() {
        var limiter = new LoginAttemptLimiter(1, 900);
        limiter.recordFailure("clinica-a", "ana@x.com");
        assertTrue(limiter.retryAfterSeconds("clinica-a", "ana@x.com") > 0);
        assertEquals(0, limiter.retryAfterSeconds("clinica-b", "ana@x.com"));
        assertEquals(0, limiter.retryAfterSeconds("clinica-a", "bia@x.com"));
    }

    @Test
    void successClearsTheCounter() {
        var limiter = new LoginAttemptLimiter(2, 900);
        limiter.recordFailure("c", "a@x.com");
        limiter.recordSuccess("c", "a@x.com");
        limiter.recordFailure("c", "a@x.com");
        assertEquals(0, limiter.retryAfterSeconds("c", "a@x.com"));
    }

    @Test
    void blockExpiresWithTheWindow() throws InterruptedException {
        var limiter = new LoginAttemptLimiter(1, 1);
        limiter.recordFailure("c", "a@x.com");
        assertTrue(limiter.retryAfterSeconds("c", "a@x.com") > 0);
        Thread.sleep(1100);
        assertEquals(0, limiter.retryAfterSeconds("c", "a@x.com"));
    }
}
