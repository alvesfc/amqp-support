package br.com.pexin.amqp.samples.producer;


import br.com.pexin.amqp.AmqpRabbitProducer;
import br.com.pexin.amqp.annotation.AmqpProducer;
import br.com.pexin.amqp.annotation.AmqpQueue;
import br.com.pexin.amqp.samples.producer.model.TestSimpleMessage;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
@AmqpProducer(
        amqpQueue = @AmqpQueue(
                name = "simple.queue.2"
        )
)
public class TestSimpleQueue2Producer extends AmqpRabbitProducer<TestSimpleMessage> {

    @Override
    public void beforeSend(TestSimpleMessage message) {
        System.out.println("beforeSend");
    }

    @Override
    public void afterSend(TestSimpleMessage message) {
        System.out.println("afterSend");
    }
}
