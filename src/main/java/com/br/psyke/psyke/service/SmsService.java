package com.br.psyke.psyke.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    private boolean enabled;

    @PostConstruct
    void init() {
        enabled = !accountSid.isBlank() && !authToken.isBlank() && !fromNumber.isBlank();
        if (enabled) {
            Twilio.init(accountSid, authToken);
            log.info("SMS service initialized with Twilio");
        } else {
            log.warn("SMS service disabled - configure twilio.* properties to enable");
        }
    }

    public void send(String to, String text) {
        if (!enabled) {
            log.info("[SMS FALLBACK] To: {}, Text: {}", to, text);
            return;
        }
        try {
            Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber), text).create();
            log.info("SMS sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
        }
    }

    public boolean isEnabled() { return enabled; }
}
