package com.gymcrm.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "twilio")
public class TwilioConfig {

    private String accountSid;
    private String authToken;
    private String whatsappFrom;
    private String smsFrom;
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled && accountSid != null && !accountSid.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized — WhatsApp from: {}", whatsappFrom);
        } else {
            log.warn("Twilio is DISABLED — reminders will be logged only. Set TWILIO_ENABLED=true to activate.");
        }
    }
}
