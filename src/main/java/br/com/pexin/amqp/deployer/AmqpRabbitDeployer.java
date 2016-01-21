package br.com.pexin.amqp.deployer;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
@Configurable
public class AmqpRabbitDeployer {

    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public AmqpRabbitDeployer(final RabbitAdmin rabbitAdmin, final RabbitTemplate rabbitTemplate){
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void deployQueueWithNewConfiguration(Queue oldQueue) {
        final Queue aleatoryQueue = generateAleatoryQueue(oldQueue.getName());
        moveMessages(oldQueue, aleatoryQueue);
        final Queue newQueue = deleteAndRedeclareQueue(oldQueue);
        moveMessages(aleatoryQueue, newQueue);
        rabbitAdmin.deleteQueue(aleatoryQueue.getName());
    }

    private Queue generateAleatoryQueue(String oldQueueName) {
        String aleatoryName = oldQueueName + UUID.randomUUID().getLeastSignificantBits();
        Queue queue = new Queue(aleatoryName, true, true, false);
        rabbitAdmin.declareQueue(queue);
        return queue;
    }

    private Queue deleteAndRedeclareQueue(Queue oldQueue) {
        if (rabbitAdmin.deleteQueue(oldQueue.getName())) {
            rabbitAdmin.declareQueue(oldQueue);
            return oldQueue;
        } else {
            throw new AmqpException("We cant delete the old queue!");
        }
    }

    private void moveMessages(Queue queueConsumed, Queue queueProcessed) {
        try {
            sendsAllMessagesToTheQueue(queueProcessed, consumeAllTheMessagesInTheQueue(queueConsumed));
        } catch (AmqpException ex) {
            throw new AmqpException("Error in deploy a queue with new configuration. ", ex);
        }
    }

    private void sendsAllMessagesToTheQueue(Queue queue, List<Message> listMessages) throws AmqpException {
        listMessages.stream()
                .forEach(m -> rabbitTemplate.send(queue.getName(), m));
    }

    private List<Message> consumeAllTheMessagesInTheQueue(Queue queue) throws AmqpException {
        final List<Message> listMessages = new ArrayList<>();
        while (true) {
            Message message = rabbitTemplate.receive(queue.getName());
            if (message == null) {
                break;
            }
            listMessages.add(message);
        }
        return listMessages;
    }
}
