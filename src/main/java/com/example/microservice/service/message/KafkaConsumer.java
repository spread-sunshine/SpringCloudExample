package com.example.microservice.service.message;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumer {

    @KafkaListener(
            topics = "${spring.kafka.topics.user-events:user.events}",
            groupId = "${spring.kafka.consumer.group-id:microservice-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenUserEvents(
            @Payload Object message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        
        try {
            log.info("Received user event from topic '{}', partition {}, offset {}: {}",
                    topic, partition, offset, message);
            
            // Process the message
            processUserEvent(message);
            
            // Manually acknowledge the message
            ack.acknowledge();
            log.debug("Acknowledged message from topic '{}', partition {}, offset {}",
                    topic, partition, offset);
            
        } catch (Exception e) {
            log.error("Error processing message from topic '{}', partition {}, offset {}: {}",
                    topic, partition, offset, message, e);
            // Don't acknowledge, let it go to DLQ or retry
        }
    }

    @KafkaListener(
            topics = "${spring.kafka.topics.order-events:order.events}",
            groupId = "${spring.kafka.consumer.group-id:microservice-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenOrderEvents(
            ConsumerRecord<String, Object> record,
            Acknowledgment ack) {
        
        try {
            log.info("Received order event: key={}, value={}, topic={}, partition={}, offset={}",
                    record.key(), record.value(), record.topic(), record.partition(), record.offset());
            
            // Process the order event
            processOrderEvent(record.value());
            
            // Manually acknowledge
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing order event: key={}", record.key(), e);
            // Consider sending to DLQ or retry topic
        }
    }

    @KafkaListener(
            topics = "${spring.kafka.topics.dlq:dlq.topic}",
            groupId = "${spring.kafka.consumer.group-id:microservice-group}-dlq",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenDeadLetterQueue(
            @Payload Object message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.error("Received dead letter message from topic '{}', partition {}, offset {}: {}",
                topic, partition, offset, message);
        
        // Handle dead letter (alert, store, analyze)
        handleDeadLetter(message, topic, partition, offset);
    }

    private void processUserEvent(Object event) {
        // Implement user event processing logic
        log.debug("Processing user event: {}", event);
    }

    private void processOrderEvent(Object event) {
        // Implement order event processing logic
        log.debug("Processing order event: {}", event);
    }

    private void handleDeadLetter(Object message, String topic, int partition, long offset) {
        // Implement dead letter handling logic
        log.warn("Handling dead letter: topic={}, partition={}, offset={}, message={}",
                topic, partition, offset, message);
        // Could send alert, store to database for analysis
    }
}