package com.jalaldeveloper.accountingsystem.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitMessagingConfiguration {

    @Bean
    TopicExchange integrationEventExchange(MessagingProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    Declarables integrationEventTopology(TopicExchange integrationEventExchange) {
        DirectExchange dlx = new DirectExchange("accounting.events.dlx", true, false);
        Queue purchaseInventorySync = durableQueue("purchase.inventory-sync.q");
        Queue salesInventorySync = durableQueue("sales.inventory-sync.q");
        Queue deadLetterQueue = new Queue("accounting.events.dlq", true);

        return new Declarables(
                dlx, purchaseInventorySync, salesInventorySync, deadLetterQueue,
                BindingBuilder.bind(deadLetterQueue).to(dlx).with("integration.dead"),
                bind(purchaseInventorySync, integrationEventExchange, "inventory.stock-picking.validated"),
                bind(salesInventorySync, integrationEventExchange, "inventory.stock-picking.validated")
        );
    }

    @Bean
    MessageConverter integrationMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter integrationMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(integrationMessageConverter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter integrationMessageConverter,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(integrationMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    private static Queue durableQueue(String name) {
        return QueueBuilder.durable(name)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", "accounting.events.dlx",
                        "x-dead-letter-routing-key", "integration.dead"))
                .build();
    }

    private static Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
