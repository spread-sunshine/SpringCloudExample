package com.example.microservice.service.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMQConsumer {

    @RabbitListener(queues = "${spring.rabbitmq.queue:microservice.queue}")
    public void receiveMessage(@Payload Object message) {
        try {
            log.info("Received message: {}", message);
            // Process the message here
            processMessage(message);
        } catch (Exception e) {
            log.error("Error processing message: {}", message, e);
            throw e; // Will go to DLQ if configured
        }
    }

    @RabbitListener(queues = "${spring.rabbitmq.dead-letter-queue:microservice.dlq}")
    public void receiveDeadLetterMessage(@Payload Object message) {
        log.error("Received dead letter message: {}", message);
        // Handle dead letter messages (alert, store, retry, etc.)
        handleDeadLetter(message);
    }

    private void processMessage(Object message) {
        // Implement business logic here
        log.debug("Processing message: {}", message);
    }

    private void handleDeadLetter(Object message) {
        // Implement dead letter handling logic
        log.warn("Handling dead letter message: {}", message);
        // Could send alert, store to database, or schedule retry
    }
}