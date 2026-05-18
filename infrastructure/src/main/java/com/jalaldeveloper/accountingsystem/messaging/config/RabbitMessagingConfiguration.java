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
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMessagingConfiguration {

    @Bean
    TopicExchange integrationEventExchange(MessagingProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    Declarables integrationEventTopology(TopicExchange integrationEventExchange) {
        DirectExchange dlx = new DirectExchange("accounting.events.dlx", true, false);
        Queue accounting = durableQueue("accounting.events.q");
        Queue purchase = durableQueue("purchase.events.q");
        Queue sales = durableQueue("sales.events.q");
        Queue inventory = durableQueue("inventory.events.q");
        Queue contacts = durableQueue("contacts.events.q");
        Queue deadLetterQueue = new Queue("accounting.events.dlq", true);

        return new Declarables(
                dlx, accounting, purchase, sales, inventory, contacts, deadLetterQueue,
                BindingBuilder.bind(deadLetterQueue).to(dlx).with("integration.dead"),
                bind(accounting, integrationEventExchange, "accounting.#"),
                bind(accounting, integrationEventExchange, "purchase.#"),
                bind(accounting, integrationEventExchange, "inventory.#"),
                bind(accounting, integrationEventExchange, "contacts.#"),
                bind(purchase, integrationEventExchange, "purchase.#"),
                bind(purchase, integrationEventExchange, "inventory.stock-picking.validated"),
                bind(sales, integrationEventExchange, "sales.#"),
                bind(sales, integrationEventExchange, "inventory.stock-picking.validated"),
                bind(inventory, integrationEventExchange, "inventory.#"),
                bind(contacts, integrationEventExchange, "contacts.#")
        );
    }

    @Bean
    MessageConverter integrationMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter integrationMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(integrationMessageConverter);
        factory.setDefaultRequeueRejected(false);
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
