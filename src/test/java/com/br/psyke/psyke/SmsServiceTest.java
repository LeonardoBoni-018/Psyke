package com.br.psyke.psyke;

import com.br.psyke.psyke.service.SmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Test
    void shouldFallbackToLoggingWhenTwilioNotConfigured() {
        var smsService = new SmsService();
        ReflectionTestUtils.setField(smsService, "accountSid", "");
        ReflectionTestUtils.setField(smsService, "authToken", "");
        ReflectionTestUtils.setField(smsService, "fromNumber", "");
        ReflectionTestUtils.invokeMethod(smsService, "init");
        assertDoesNotThrow(() -> smsService.send("+5511999999999", "Teste SMS"));
    }

    @Test
    void shouldBeDisabledByDefault() {
        var smsService = new SmsService();
        ReflectionTestUtils.setField(smsService, "accountSid", "");
        ReflectionTestUtils.setField(smsService, "authToken", "");
        ReflectionTestUtils.setField(smsService, "fromNumber", "");
        ReflectionTestUtils.invokeMethod(smsService, "init");
        assertFalse(smsService.isEnabled());
    }
}
