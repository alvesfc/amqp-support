package br.com.uol.ps.amqp.support.factory;

import br.com.uol.ps.amqp.support.annotation.AmqpExchange;
import br.com.uol.ps.amqp.support.annotation.AmqpQueue;
import org.springframework.amqp.core.Queue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
public class AmqpQueueFactory {

    private final static String DLQ_EXCHANGE = "x-dead-letter-exchange";
    private final static String DLQ_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String SEPARATOR = "_";
    public static final String QUEUE_POSTFIX = "queue";
    public static final String DLQ_POSTFIX = "dlq";
    public static final String WAITING_POSTFIX = "waiting";

    public static Map<String, Object> retrieveArgsFromQueue(final AmqpQueue amqpQueue) {
        return new HashMap<String, Object>() {
            {
                put(DLQ_EXCHANGE, AmqpExchangeFactory.retrieveDlqExchangeName(amqpQueue.name()));
                put(DLQ_ROUTING_KEY, AmqpExchangeFactory.retrieveExchangeDlqRoutingKey(amqpQueue.name()));
            }
        };
    }

    public static Map<String, Object> retrieveArgsFromQueueToWaiting(AmqpQueue amqpQueue, AmqpExchange amqpExchange) {
        return new HashMap<String, Object>() {
            {
                put(DLQ_EXCHANGE,
                        AmqpExchangeFactory.retrieveExchangeName(amqpQueue.name(), amqpExchange.amqpExchangeType()));
                put(DLQ_ROUTING_KEY, AmqpExchangeFactory.retrieveExchangeRoutingKey(amqpQueue.name()));
            }
        };
    }

    public static String retrieveQueueName(final AmqpQueue amqpQueue) {
        return new StringBuilder(amqpQueue.name()).append(SEPARATOR).append(QUEUE_POSTFIX).toString();
    }

    public static String retrieveDlqQueueName(final AmqpQueue amqpQueue) {
        return new StringBuilder(retrieveQueueName(amqpQueue)).append(SEPARATOR).append(DLQ_POSTFIX).toString();
    }

    public static String retrieveWaitingQueueName(final AmqpQueue amqpQueue) {
        return new StringBuilder(retrieveQueueName(amqpQueue)).append(SEPARATOR).append(WAITING_POSTFIX).toString();
    }

    public static Queue createQueue(final AmqpQueue amqpQueue) {
        return new Queue(retrieveQueueName(amqpQueue), amqpQueue.durable(), amqpQueue.exclusive(),
                amqpQueue.autoDelete(),
                AmqpQueueFactory.retrieveArgsFromQueue(amqpQueue));
    }

    public static Queue createDlqQueue(AmqpQueue amqpQueue) {
        return new Queue(retrieveDlqQueueName(amqpQueue), amqpQueue.durable(), amqpQueue.exclusive(),
                amqpQueue.autoDelete());
    }

    public static Queue createWaitingQueue(AmqpQueue amqpQueue, AmqpExchange amqpExchange) {
        return new Queue(retrieveWaitingQueueName(amqpQueue), amqpQueue.durable(), amqpQueue.exclusive(),
                amqpQueue.autoDelete(), AmqpQueueFactory.retrieveArgsFromQueueToWaiting(amqpQueue, amqpExchange));
    }
}
