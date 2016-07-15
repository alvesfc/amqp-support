package br.com.uol.ps.amqp.support.samples.producer;


import br.com.uol.ps.amqp.support.AmqpRabbitProducer;
import br.com.uol.ps.amqp.support.annotation.AmqpProducer;
import br.com.uol.ps.amqp.support.annotation.AmqpQueue;
import br.com.uol.ps.amqp.support.samples.producer.model.TestSimpleMessage;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
@AmqpProducer(
        delayTimeInMillis = 15000,
        amqpQueue = @AmqpQueue(
                name = "simple.queue.1"
        )
)
public class TestSimpleQueue1WithDelayProducer extends AmqpRabbitProducer<TestSimpleMessage> {

    @Override
    public void beforeSend(TestSimpleMessage message) {
        System.out.println("beforeSend");
    }

    @Override
    public void afterSend(TestSimpleMessage message) {
        System.out.println("afterSend");
    }
}
