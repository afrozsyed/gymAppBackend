package com.gymcrm.service;

import com.gymcrm.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final TwilioConfig twilioConfig;

    public void sendReminder(String toPhone, String memberName, String expiryDate) {
        if (!twilioConfig.isEnabled()) {
            log.info("[MOCK] Reminder to {} ({}): membership expires {}", memberName, toPhone, expiryDate);
            return;
        }

        String body = String.format(
                "Hi %s! Your gym membership expires on %s. Please renew to continue your fitness journey. 💪",
                memberName, expiryDate
        );
        String formattedPhone = normalizePhone(toPhone);

        try {
            Message.creator(
                    new PhoneNumber("whatsapp:" + formattedPhone),
                    new PhoneNumber(twilioConfig.getWhatsappFrom()),
                    body
            ).create();
            log.info("[WHATSAPP] Sent reminder to {} ({})", memberName, formattedPhone);
        } catch (Exception waEx) {
            log.warn("[WHATSAPP] Failed for {} — falling back to SMS: {}", memberName, waEx.getMessage());
            try {
                Message.creator(
                        new PhoneNumber(formattedPhone),
                        new PhoneNumber(twilioConfig.getSmsFrom()),
                        body
                ).create();
                log.info("[SMS] Sent reminder to {} ({})", memberName, formattedPhone);
            } catch (Exception smsEx) {
                log.error("[SMS] Also failed for {}: {}", memberName, smsEx.getMessage());
            }
        }
    }

    private String normalizePhone(String phone) {
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
    }
}
