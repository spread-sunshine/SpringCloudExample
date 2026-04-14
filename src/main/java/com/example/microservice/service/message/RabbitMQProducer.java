package com.example.microservice.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(String routingKey, Object message) {
        try {
            rabbitTemplate.convertAndSend(routingKey, message);
            log.debug("Message sent to routing key '{}': {}", routingKey, message);
        } catch (Exception e) {
            log.error("Failed to send message to routing key '{}': {}", routingKey, message, e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    public void sendMessageWithHeaders(String routingKey, Object message, Map<String, Object> headers) {
        try {
            rabbitTemplate.convertAndSend(routingKey, message, m -> {
                if (headers != null) {
                    headers.forEach(m.getMessageProperties()::setHeader);
                }
                return m;
            });
            log.debug("Message with headers sent to routing key '{}': {}", routingKey, message);
        } catch (Exception e) {
            log.error("Failed to send message with headers to routing key '{}': {}", routingKey, message, e);
            throw new RuntimeException("Failed to send message with headers", e);
        }
    }

    public void sendDelayedMessage(String routingKey, Object message, long delayMillis) {
        try {
            rabbitTemplate.convertAndSend(routingKey, message, m -> {
                m.getMessageProperties().setDelay(Math.toIntExact(delayMillis));
                return m;
            });
            log.debug("Delayed message sent to routing key '{}' with delay {}ms: {}", 
                    routingKey, delayMillis, message);
        } catch (Exception e) {
            log.error("Failed to send delayed message to routing key '{}': {}", routingKey, message, e);
            throw new RuntimeException("Failed to send delayed message", e);
        }
    }
}