package com.template.microservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
@Slf4j
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.exchange:microservice.exchange}")
    private String exchangeName;

    @Value("${spring.rabbitmq.queue:microservice.queue}")
    private String queueName;

    @Value("${spring.rabbitmq.routing-key:microservice.routing-key}")
    private String routingKey;

    @Value("${spring.rabbitmq.dead-letter-exchange:microservice.dlx}")
    private String deadLetterExchange;

    @Value("${spring.rabbitmq.dead-letter-queue:microservice.dlq}")
    private String deadLetterQueue;

    @Value("${spring.rabbitmq.dead-letter-routing-key:microservice.dlq.key}")
    private String deadLetterRoutingKey;

    // Dead Letter Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    // Dead Letter Queue
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    // Dead Letter Binding
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(deadLetterRoutingKey);
    }

    // Main Exchange
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    // Main Queue with DLQ configuration
    @Bean
    public Queue mainQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", deadLetterExchange);
        args.put("x-dead-letter-routing-key", deadLetterRoutingKey);
        args.put("x-message-ttl", 60000); // 1 minute TTL
        args.put("x-max-length", 10000); // Max queue length

        return QueueBuilder.durable(queueName)
                .withArguments(args)
                .build();
    }

    // Main Binding
    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(mainQueue())
                .to(mainExchange())
                .with(routingKey);
    }

    // JSON Message Converter
    @Bean
    public Jackson2JsonMessageConverter rabbitJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Rabbit Template with JSON converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitJsonMessageConverter());
        rabbitTemplate.setExchange(exchangeName);
        
        // Enable confirm and return callbacks
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message confirmed with correlation data: {}", correlationData);
            } else {
                log.error("Message not confirmed, cause: {}", cause);
            }
        });
        
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Message returned, replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getReplyCode(), returned.getReplyText(),
                    returned.getExchange(), returned.getRoutingKey());
        });
        
        return rabbitTemplate;
    }

    // Listener container factory
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitJsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false); // Don't requeue rejected messages
        return factory;
    }
}