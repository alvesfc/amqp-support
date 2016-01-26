package br.com.pexin.amqp.factory;

import br.com.pexin.amqp.annotation.AmqpExchange;
import br.com.pexin.amqp.annotation.AmqpExchangeType;
import br.com.pexin.amqp.annotation.AmqpProducer;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.TopicExchange;

/**
 * Created by rafaelfirmino on 20/01/16.
 */
public class AmqpExchangeFactory {

    public static String retrieveExchangeName(final String queueName, AmqpExchangeType amqpExchangeType){
        switch (amqpExchangeType){
            case DIRECT:
                return new StringBuilder(queueName).append(".direct").toString();
            case TOPIC:
                return new StringBuilder(queueName).append(".topic").toString();
            default:
                throw new RuntimeException("Fail from retrieveExchangeName.");
        }
    }

    public static String retrieveDlqExchangeName(final String queueName){
        return new StringBuilder(queueName).append(".direct.dlq").toString();
    }

    public static String retrieveExchangeDlqRoutingKey(final String queueName){
        return new StringBuilder(queueName).append(".dlq.key").toString();
    }

    public static String retrieveExchangeRoutingKey(final String queueName){
        return new StringBuilder(queueName).append(".key").toString();
    }

    public static String retrieveExchangeWaitingRoutingKey(final String queueName){
        return new StringBuilder(queueName).append(".waiting.key").toString();
    }

    public static Exchange buildDirectExchange(AmqpProducer amqpProducer) {
        final String exchangeName = retrieveExchangeName(amqpProducer.amqpQueue().name(), amqpProducer.amqpExchange().amqpExchangeType());
        final AmqpExchange amqpExchange = amqpProducer.amqpExchange();
        return new DirectExchange(exchangeName, amqpExchange.durable(), amqpExchange.autoDelete());
    }

    public static Exchange buildTopicExchange(AmqpProducer amqpProducer) {
        final String exchangeName = retrieveExchangeName(amqpProducer.amqpQueue().name(), amqpProducer.amqpExchange().amqpExchangeType());
        final AmqpExchange amqpExchange = amqpProducer.amqpExchange();
        return new TopicExchange(exchangeName, amqpExchange.durable(), amqpExchange.autoDelete());
    }

    public static Exchange buildDirectDlqExchange(AmqpProducer amqpProducer) {
        final String exchangeName = retrieveDlqExchangeName(amqpProducer.amqpQueue().name());
        final AmqpExchange amqpExchange = amqpProducer.amqpExchange();
        return new DirectExchange(exchangeName, amqpExchange.durable(), amqpExchange.autoDelete());
    }

    public static String retrieveExchangeRoutingKeyGenericToTopic() {
        return new String("#");
    }
}
