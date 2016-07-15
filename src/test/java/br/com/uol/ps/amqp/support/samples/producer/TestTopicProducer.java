package br.com.uol.ps.amqp.support.samples.producer;

import br.com.uol.ps.amqp.support.AmqpRabbitProducer;
import br.com.uol.ps.amqp.support.annotation.AmqpExchange;
import br.com.uol.ps.amqp.support.annotation.AmqpExchangeType;
import br.com.uol.ps.amqp.support.annotation.AmqpProducer;
import br.com.uol.ps.amqp.support.annotation.AmqpQueue;
import br.com.uol.ps.amqp.support.samples.producer.model.TestSimpleMessage;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
@AmqpProducer(
        amqpQueue = @AmqpQueue(
                name = "simple.queue.1 and simple.queue.2"
        ),
        amqpExchange = @AmqpExchange(
                amqpExchangeType = AmqpExchangeType.TOPIC
        )
)
public class TestTopicProducer extends AmqpRabbitProducer<TestSimpleMessage>{

    @Override
    public void beforeSend(TestSimpleMessage message) {
        System.out.println("beforeSend Topic");
    }

    @Override
    public void afterSend(TestSimpleMessage message) {
        System.out.println("afterSend Topic");
    }
}
