package com.example.grpcdemo.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Basic RabbitMQ configuration declaring queues and JSON message conversion.
 */
@Configuration
@EnableRabbit
@EnableRetry
@Profile("!test")
public class RabbitMQConfig {

    public static final String INTERVIEW_COMPLETED_QUEUE = "interview.completed.queue";
    public static final String REPORT_GENERATED_QUEUE = "report.generated.queue";

    @Bean
    public Queue interviewCompletedQueue() {
        return new Queue(INTERVIEW_COMPLETED_QUEUE, true);
    }

    @Bean
    public Queue reportGeneratedQueue() {
        return new Queue(REPORT_GENERATED_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
