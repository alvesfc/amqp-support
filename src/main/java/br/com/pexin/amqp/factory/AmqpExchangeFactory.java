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

    public static final String SEPARATOR = ".";
    public static final String DLQ_POSTFIX = "dlq";
    public static final String WAITING_POSTFIX = "waiting";
    public static final String KEY = "key";
    public static final String DIRECT = "direct";
    public static final String TOPIC = "topic";

    public static String retrieveExchangeName(final String queueName, AmqpExchangeType amqpExchangeType){
        switch (amqpExchangeType){
        case DIRECT:
            return new StringBuilder(queueName).append(SEPARATOR).append(DIRECT).toString();
        case TOPIC:
            return new StringBuilder(queueName).append(SEPARATOR).append(TOPIC).toString();
        default:
            throw new RuntimeException("Fail from retrieveExchangeName.");
        }
    }

    public static String retrieveDlqExchangeName(final String queueName){
        return new StringBuilder(queueName).append(SEPARATOR).append(DIRECT).append(SEPARATOR).append(DLQ_POSTFIX).toString();
    }

    public static String retrieveExchangeDlqRoutingKey(final String queueName){
        return new StringBuilder(queueName).append(SEPARATOR).append(DLQ_POSTFIX).append(SEPARATOR).append(KEY).toString();
    }

    public static String retrieveExchangeRoutingKey(final String queueName){
        return new StringBuilder(queueName).append(SEPARATOR).append(KEY).toString();
    }

    public static String retrieveExchangeWaitingRoutingKey(final String queueName){
        return new StringBuilder(queueName).append(SEPARATOR).append(WAITING_POSTFIX).append(SEPARATOR).append(KEY).toString();
    }

    public static Exchange buildDirectExchange(AmqpProducer amqpProducer) {
        final String queueName = AmqpQueueFactory.retrieveQueueName(amqpProducer.amqpQueue());
        final String exchangeName = retrieveExchangeName(queueName, amqpProducer.amqpExchange().amqpExchangeType());
        final AmqpExchange amqpExchange = amqpProducer.amqpExchange();
        return new DirectExchange(exchangeName, amqpExchange.durable(), amqpExchange.autoDelete());
    }

    public static Exchange buildTopicExchange(AmqpProducer amqpProducer) {
        final String queueName = AmqpQueueFactory.retrieveQueueName(amqpProducer.amqpQueue());
        final String exchangeName = retrieveExchangeName(queueName, amqpProducer.amqpExchange().amqpExchangeType());
        final AmqpExchange amqpExchange = amqpProducer.amqpExchange();
        return new TopicExchange(exchangeName, amqpExchange.durable(), amqpExchange.autoDelete());
    }

    public static Exchange buildDirectDlqExchange(AmqpProducer amqpProducer) {
        final String queueName = AmqpQueueFactory.retrieveQueueName(amqpProducer.amqpQueue());
        final String exchangeName = retrieveDlqExchangeName(queueName);
        final AmqpExchange amqpExchange = amqpProducer.amqpExchange();
        return new DirectExchange(exchangeName, amqpExchange.durable(), amqpExchange.autoDelete());
    }

    public static String retrieveExchangeRoutingKeyGenericToTopic() {
        return new String("#");
    }
}
