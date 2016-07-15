package br.com.uol.ps.amqp.support;

import br.com.uol.ps.amqp.support.annotation.AmqpExchange;
import br.com.uol.ps.amqp.support.annotation.AmqpProducer;
import br.com.uol.ps.amqp.support.annotation.AmqpQueue;
import br.com.uol.ps.amqp.support.deployer.AmqpRabbitDeployer;
import br.com.uol.ps.amqp.support.factory.AmqpExchangeFactory;
import br.com.uol.ps.amqp.support.factory.AmqpQueueFactory;
import br.com.uol.ps.amqp.support.log.FluentLogger;
import br.com.uol.ps.amqp.support.post_processor.DefaultMessagePostProcessor;
import br.com.uol.ps.amqp.support.post_processor.DelayedMessagePostProcessor;
import br.com.uol.ps.amqp.support.post_processor.PersistenceMessagePostProcessor;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.uol.ps.amqp.support.annotation.AmqpExchangeType.TOPIC;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
public abstract class AmqpRabbitProducer<T> {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;

    private AmqpProducer amqpProducerMetadata;
    private FluentLogger fluentLogger = new FluentLogger(LogFactory.getLog(getClass()));

    @PostConstruct
    private void setUp() {

        populateAmqpProducerMetadata();

        switch (retrieveAmqpExchange().amqpExchangeType()) {
        case DIRECT:

            final Exchange exchange = createExchange();
            final Exchange exchangeDlq = createDlqExchange();
            final Queue queue = createOrReplaceQueue();
            final Queue dlqQueue = createOrReplaceDlqQueue();
            final Queue waitingQueue = createOrReplaceWaitingQueue();
            final List<Binding> bindings = doBindings(exchange, exchangeDlq, queue, dlqQueue, waitingQueue);
            declare(exchange, exchangeDlq, queue, dlqQueue, waitingQueue, bindings);

            return;

        case TOPIC:

            final List<String> queueNamesToTopic = retrieveQueueNames();
            final Exchange exchangeTopic = createExchange();
            final List<Binding> bindingsToTopic = doBindingsToTopic(exchangeTopic, queueNamesToTopic);
            declare(exchangeTopic, bindingsToTopic);

            return;
        }
    }

    public void sendMessage(final T message) {
        beforeSend(message);
        rabbitTemplate.convertAndSend(
                AmqpExchangeFactory.retrieveExchangeName(AmqpQueueFactory.retrieveQueueName(retrieveAmqpQueue()),
                        amqpProducerMetadata.amqpExchange().amqpExchangeType()),
                retrieveExchangeRoutingKeyToSend(),
                message,
                retrieveMessagePostProcessor(message));
        afterSend(message);
    }

    public <R> R sendSyncMessage(final T message, final Class<R> responseClazz) {

        if (isTopic()) {
            throw new RuntimeException("Topics cant send synchronous messages.");
        }

        beforeSend(message);
        R response = responseClazz
                .cast(
                        rabbitTemplate.convertSendAndReceive(
                                AmqpExchangeFactory
                                        .retrieveExchangeName(AmqpQueueFactory.retrieveQueueName(retrieveAmqpQueue()),
                                                amqpProducerMetadata.amqpExchange().amqpExchangeType()),
                                AmqpExchangeFactory.retrieveExchangeRoutingKey(
                                        AmqpQueueFactory.retrieveQueueName(retrieveAmqpQueue())),
                                message
                        )
                );
        afterSend(message);

        return response;
    }

    private String retrieveExchangeRoutingKeyToSend() {
        return isTopic() ? AmqpExchangeFactory.retrieveExchangeRoutingKeyGenericToTopic() :
                hasDelay() ?
                        AmqpExchangeFactory.retrieveExchangeWaitingRoutingKey(
                                AmqpQueueFactory.retrieveQueueName(retrieveAmqpQueue())) :
                        AmqpExchangeFactory
                                .retrieveExchangeRoutingKey(AmqpQueueFactory.retrieveQueueName(retrieveAmqpQueue()));
    }

    private boolean hasDelay() {
        return amqpProducerMetadata.delayTimeInMillis() > 0;
    }

    private boolean hasDeliveryModePersistence() {
        return MessageDeliveryMode.PERSISTENT.equals(amqpProducerMetadata.deliveryMode());
    }

    private boolean isTopic() {
        return TOPIC.equals(retrieveAmqpExchange().amqpExchangeType());
    }

    private MessagePostProcessor retrieveMessagePostProcessor(final T message) {
        MessagePostProcessor messagePostProcessor = new DefaultMessagePostProcessor(message);

        if (hasDelay()) {
            messagePostProcessor = new DelayedMessagePostProcessor(amqpProducerMetadata.delayTimeInMillis());
        } else if (hasDeliveryModePersistence()) {
            messagePostProcessor = new PersistenceMessagePostProcessor(amqpProducerMetadata.deliveryMode());
        }
        return messagePostProcessor;
    }

    private void declare(Exchange exchangeTopic, List<Binding> bindingsToTopic) {
        rabbitAdmin.declareExchange(exchangeTopic);
        bindingsToTopic.stream()
                .forEach(b -> rabbitAdmin.declareBinding(b));
    }

    private List<Binding> doBindingsToTopic(Exchange exchangeTopic, List<String> queueNamesToTopic) {

        return queueNamesToTopic.stream()
                .map(q -> new Queue(q))
                .collect(Collectors.toList())
                .stream()
                .map(q -> BindingBuilder.bind(q).to(exchangeTopic)
                        .with(AmqpExchangeFactory.retrieveExchangeRoutingKeyGenericToTopic()).noargs())
                .collect(Collectors.toList());
    }

    private List<String> retrieveQueueNames() {
        final List<String> queueNames = new ArrayList<>();
        final String[] names = AmqpQueueFactory.retrieveQueueName(retrieveAmqpQueue()).split(" and ");
        if (names != null) {
            queueNames.addAll(Arrays.asList(names));
        }
        return queueNames;
    }

    private void declare(Exchange exchange, Exchange exchangeDlq, Queue queue, Queue dlqQueue, Queue waitingQueue,
            List<Binding> bindings) {
        try {
            declareQueue(queue);
            declareQueue(dlqQueue);
            declareQueue(waitingQueue);
            rabbitAdmin.declareExchange(exchange);
            rabbitAdmin.declareExchange(exchangeDlq);
            bindings.stream()
                    .forEach(b -> rabbitAdmin.declareBinding(b));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void declareQueue(Queue queue) {
        try {
            rabbitAdmin.declareQueue(queue);
        } catch (AmqpIOException ex) {
            fluentLogger
                    .key("method")
                    .value("declareQueue")
                    .key("cause")
                    .value("Starting the deployment of queue " + queue.getName() + " with a new configuration.")
                    .key("reason_deploy")
                    .value(ex.getCause().getCause())
                    .logInfo();
            new AmqpRabbitDeployer(rabbitAdmin, rabbitTemplate)
                    .deployQueueWithNewConfiguration(queue);
        }
    }

    private List<Binding> doBindings(Exchange exchange, Exchange exchangeDlq, Queue queue, Queue dlqQueue,
            Queue waitingQueue) {
        final Binding bindingExchangeToQueue = BindingBuilder.bind(queue).to(exchange)
                .with(AmqpExchangeFactory.retrieveExchangeRoutingKey(queue.getName())).noargs();
        final Binding bindingExchangeToDlqQueue = BindingBuilder.bind(dlqQueue).to(exchangeDlq)
                .with(AmqpExchangeFactory.retrieveExchangeDlqRoutingKey(queue.getName())).noargs();
        final Binding bindingExchangeToWaitingQueue = BindingBuilder.bind(waitingQueue).to(exchange)
                .with(AmqpExchangeFactory.retrieveExchangeWaitingRoutingKey(queue.getName())).noargs();
        return Arrays.asList(bindingExchangeToQueue, bindingExchangeToDlqQueue, bindingExchangeToWaitingQueue);
    }

    private Queue createOrReplaceDlqQueue() {
        return AmqpQueueFactory.createDlqQueue(retrieveAmqpQueue());
    }

    private Queue createOrReplaceQueue() {
        return AmqpQueueFactory.createQueue(retrieveAmqpQueue());
    }

    private Queue createOrReplaceWaitingQueue() {
        return AmqpQueueFactory.createWaitingQueue(retrieveAmqpQueue(), retrieveAmqpExchange());
    }

    private Exchange createExchange() {
        switch (retrieveAmqpExchange().amqpExchangeType()) {
        case DIRECT:
            return AmqpExchangeFactory.buildDirectExchange(amqpProducerMetadata);
        case TOPIC:
            return AmqpExchangeFactory.buildTopicExchange(amqpProducerMetadata);
        default:
            throw new RuntimeException("Fail from createExchange. No type defined.");
        }
    }

    private Exchange createDlqExchange() {
        return AmqpExchangeFactory.buildDirectDlqExchange(amqpProducerMetadata);
    }

    private AmqpQueue retrieveAmqpQueue() {
        return amqpProducerMetadata.amqpQueue();
    }

    private AmqpExchange retrieveAmqpExchange() {
        return amqpProducerMetadata.amqpExchange();
    }

    private void populateAmqpProducerMetadata() {
        amqpProducerMetadata = this.getClass().getAnnotation(AmqpProducer.class);
    }

    public abstract void beforeSend(final T message);

    public abstract void afterSend(final T message);
}
