package br.com.amqp.factory;

import br.com.amqp.annotation.AmqpExchange;
import br.com.amqp.annotation.AmqpQueue;
import org.springframework.amqp.core.Queue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
public class AmqpQueueFactory {

    private final static String DLQ_EXCHANGE = "x-dead-letter-exchange";
    private final static String DLQ_ROUTING_KEY = "x-dead-letter-routing-key";

    public static Map<String, Object> retrieveArgsFromQueue(final AmqpQueue amqpQueue){
        return new HashMap<String, Object>(){
            {
                put(DLQ_EXCHANGE, AmqpExchangeFactory.retrieveDlqExchangeName(amqpQueue.name()));
                put(DLQ_ROUTING_KEY, AmqpExchangeFactory.retrieveExchangeDlqRoutingKey(amqpQueue.name()));
            }
        };
    }

    public static Map<String, Object> retrieveArgsFromQueueToWaiting(AmqpQueue amqpQueue, AmqpExchange amqpExchange) {
        return new HashMap<String, Object>(){
            {
                put(DLQ_EXCHANGE, AmqpExchangeFactory.retrieveExchangeName(amqpQueue.name(), amqpExchange.amqpExchangeType()));
                put(DLQ_ROUTING_KEY, AmqpExchangeFactory.retrieveExchangeRoutingKey(amqpQueue.name()));
            }
        };
    }

    public static String retrieveDlqQueueName(final AmqpQueue amqpQueue){
        return new StringBuilder(amqpQueue.name()).append(".dlq").toString();
    }

    public static String retrieveWaitingQueueName(final AmqpQueue amqpQueue){
        return new StringBuilder(amqpQueue.name()).append(".waiting").toString();
    }

    public static Queue createQueue(final AmqpQueue amqpQueue){
        return new Queue(amqpQueue.name(), amqpQueue.durable(), amqpQueue.exclusive(), amqpQueue.autoDelete(), AmqpQueueFactory.retrieveArgsFromQueue(amqpQueue));
    }

    public static Queue createDlqQueue(AmqpQueue amqpQueue) {
        return new Queue(retrieveDlqQueueName(amqpQueue), amqpQueue.durable(), amqpQueue.exclusive(), amqpQueue.autoDelete());
    }

    public static Queue createWaitingQueue(AmqpQueue amqpQueue, AmqpExchange amqpExchange) {
        return new Queue(retrieveWaitingQueueName(amqpQueue), amqpQueue.durable(), amqpQueue.exclusive(), amqpQueue.autoDelete(), AmqpQueueFactory.retrieveArgsFromQueueToWaiting(amqpQueue, amqpExchange));
    }
}
