package br.com.uol.ps.amqp.support.samples.consumer;

import br.com.uol.ps.amqp.support.AmqpRabbitConsumer;
import br.com.uol.ps.amqp.support.annotation.AmqpConsumer;
import br.com.uol.ps.amqp.support.annotation.AmqpQueue;
import br.com.uol.ps.amqp.support.annotation.AmqpRetryPolicy;
import br.com.uol.ps.amqp.support.samples.producer.model.TestSimpleMessage;

/**
 * Created by rafaelfirmino on 19/01/16.
 */
@AmqpConsumer(
        amqpQueue = @AmqpQueue(
                name = "simple.queue.1"
        ),
        amqpDlqQueue = @AmqpQueue(
                name = "simple.queue.1.dlq"
        ),
        amqpRetryPolicy = @AmqpRetryPolicy(
                maxRetryAttemps = 5,
                timeToRetryInMillis = 2000
        ),
        concurrentConsumers = 5
)
public class TestSimpleConsumerConsumer1 extends AmqpRabbitConsumer<TestSimpleMessage, String> {

    @Override
    public String onMessage(TestSimpleMessage teste) {
        System.out.println(teste + " Consumer 1");
        return "OK Response1";
    }
}
