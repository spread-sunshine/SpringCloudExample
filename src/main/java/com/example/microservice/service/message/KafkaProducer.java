package com.example.microservice.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;


import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> sendMessage(String topic, Object message) {
        log.debug("Sending message to topic '{}': {}", topic, message);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Message sent successfully to topic '{}', partition {}, offset {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic '{}': {}", topic, message, ex);
            }
        });
        
        return future;
    }

    public CompletableFuture<SendResult<String, Object>> sendMessage(String topic, String key, Object message) {
        log.debug("Sending message to topic '{}' with key '{}': {}", topic, key, message);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Message sent successfully to topic '{}' with key '{}', partition {}, offset {}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic '{}' with key '{}': {}", 
                        topic, key, message, ex);
            }
        });
        
        return future;
    }

    public CompletableFuture<SendResult<String, Object>> sendMessageWithCallback(
            String topic, Object message, Runnable successCallback, Runnable failureCallback) {
        
        log.debug("Sending message with callback to topic '{}': {}", topic, message);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Message sent successfully with callback to topic '{}'", topic);
                if (successCallback != null) {
                    successCallback.run();
                }
            } else {
                log.error("Failed to send message with callback to topic '{}': {}", topic, message, ex);
                if (failureCallback != null) {
                    failureCallback.run();
                }
            }
        });
        
        return future;
    }
}