package br.com.pexin.amqp.post_processor;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;

public class PersistenceMessagePostProcessor implements MessagePostProcessor {

    private final MessageDeliveryMode deliveryMode;

    public PersistenceMessagePostProcessor(MessageDeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        message.getMessageProperties().setDeliveryMode(deliveryMode);
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PersistenceMessagePostProcessor))
            return false;

        PersistenceMessagePostProcessor that = (PersistenceMessagePostProcessor) o;

        return deliveryMode == that.deliveryMode;

    }

    @Override
    public int hashCode() {
        return deliveryMode != null ? deliveryMode.hashCode() : 0;
    }

}
