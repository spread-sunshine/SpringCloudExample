package com.template.microservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class AlertConfig {

    @Value("${alerts.enabled:true}")
    private boolean alertsEnabled;

    @Value("${alerts.slack.webhook-url:}")
    private String slackWebhookUrl;

    @Value("${alerts.email.recipients:}")
    private String emailRecipients;

    @Bean
    public AlertService alertService() {
        return new AlertService(alertsEnabled, slackWebhookUrl, emailRecipients);
    }

    public static class AlertService {
        private final boolean enabled;
        private final String slackWebhookUrl;
        private final String emailRecipients;
        private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();

        public AlertService(boolean enabled, String slackWebhookUrl, String emailRecipients) {
            this.enabled = enabled;
            this.slackWebhookUrl = slackWebhookUrl;
            this.emailRecipients = emailRecipients;
        }

        public void sendAlert(String type, String message, AlertLevel level) {
            if (!enabled) {
                log.debug("Alerts disabled, not sending: {} - {}", type, message);
                return;
            }

            // Rate limiting: don't send same alert type more than once per minute
            long now = System.currentTimeMillis();
            Long lastTime = lastAlertTime.get(type);
            if (lastTime != null && (now - lastTime) < 60000) {
                log.debug("Rate limiting alert type: {}", type);
                return;
            }

            try {
                // In production, integrate with actual alerting services
                // For now, just log
                switch (level) {
                    case CRITICAL:
                        log.error("CRITICAL ALERT [{}]: {}", type, message);
                        break;
                    case ERROR:
                        log.error("ERROR ALERT [{}]: {}", type, message);
                        break;
                    case WARNING:
                        log.warn("WARNING ALERT [{}]: {}", type, message);
                        break;
                    case INFO:
                        log.info("INFO ALERT [{}]: {}", type, message);
                        break;
                }

                // Simulate sending to Slack
                if (!slackWebhookUrl.isEmpty()) {
                    log.debug("Would send to Slack: {} - {}", type, message);
                }

                // Simulate sending email
                if (!emailRecipients.isEmpty()) {
                    log.debug("Would send email to {}: {} - {}", emailRecipients, type, message);
                }

                lastAlertTime.put(type, now);

            } catch (Exception e) {
                log.error("Failed to send alert: {} - {}", type, message, e);
            }
        }

        public enum AlertLevel {
            CRITICAL, ERROR, WARNING, INFO
        }
    }
}