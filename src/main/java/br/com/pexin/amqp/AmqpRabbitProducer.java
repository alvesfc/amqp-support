package br.com.pexin.amqp;

import br.com.pexin.amqp.annotation.AmqpExchange;
import br.com.pexin.amqp.annotation.AmqpExchangeType;
import br.com.pexin.amqp.annotation.AmqpProducer;
import br.com.pexin.amqp.annotation.AmqpQueue;
import br.com.pexin.amqp.deployer.AmqpRabbitDeployer;
import br.com.pexin.amqp.factory.AmqpExchangeFactory;
import br.com.pexin.amqp.factory.AmqpQueueFactory;
import br.com.pexin.amqp.post.processor.DefaultMessagePostProcessor;
import br.com.pexin.amqp.post.processor.DelayedMessagePostProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rafaelfirmino on 21/01/16.
 */
public abstract class AmqpRabbitProducer<T> {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;

    private AmqpProducer amqpProducerMetadata;

    private final Log logger = LogFactory.getLog(getClass());

    @PostConstruct
    private void setUp(){

        populateAmqpProducerMetadata();

        switch (retrieveAmqpExchange().amqpExchangeType()){
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

    public void sendMessage(final T message){
        beforeSend(message);
        rabbitTemplate.convertAndSend(
                AmqpExchangeFactory.retrieveExchangeName(retrieveAmqpQueue().name(), amqpProducerMetadata.amqpExchange().amqpExchangeType()),
                retrieveExchangeRoutingKeyToSend(),
                message,
                retrieveMessagePostProcessor(message));
        afterSend(message);
    }

    public <R> R sendSyncMessage(final T message, final Class<R> responseClazz){

        if(isTopic()){
            throw new RuntimeException("Topics cant send synchronous messages.");
        }

        beforeSend(message);
        R response = responseClazz
                        .cast(
                            rabbitTemplate.convertSendAndReceive(
                            AmqpExchangeFactory.retrieveExchangeName(retrieveAmqpQueue().name(), amqpProducerMetadata.amqpExchange().amqpExchangeType()),
                            AmqpExchangeFactory.retrieveExchangeRoutingKey(retrieveAmqpQueue().name()),
                            message
                        )
        );
        afterSend(message);

        return response;
    }

    private String retrieveExchangeRoutingKeyToSend() {
        return isTopic() ? AmqpExchangeFactory.retrieveExchangeRoutingKeyGenericToTopic() :
                hasDelay() ? AmqpExchangeFactory.retrieveExchangeWaitingRoutingKey(retrieveAmqpQueue().name()) :
                        AmqpExchangeFactory.retrieveExchangeRoutingKey(retrieveAmqpQueue().name());
    }

    private boolean hasDelay(){
        return amqpProducerMetadata.delayTimeInMillis() > 0;
    }

    private boolean isTopic(){
        return AmqpExchangeType.TOPIC.equals(retrieveAmqpExchange().amqpExchangeType());
    }

    private MessagePostProcessor retrieveMessagePostProcessor(final T message) {
        MessagePostProcessor messagePostProcessor = new DefaultMessagePostProcessor(message);
        if(hasDelay()){
            messagePostProcessor = new DelayedMessagePostProcessor(amqpProducerMetadata.delayTimeInMillis());
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
                .map(q -> BindingBuilder.bind(q).to(exchangeTopic).with(AmqpExchangeFactory.retrieveExchangeRoutingKeyGenericToTopic()).noargs())
                .collect(Collectors.toList());
    }


    private List<String> retrieveQueueNames() {
        final List<String> queueNames = new ArrayList<>();
        final String[] names = retrieveAmqpQueue().name().split(" and ");
        if(names != null){
            queueNames.addAll(Arrays.asList(names));
        }
        return queueNames;
    }

    private void declare(Exchange exchange, Exchange exchangeDlq, Queue queue, Queue dlqQueue, Queue waitingQueue, List<Binding> bindings) {
        try{
            declareQueue(queue);
            declareQueue(dlqQueue);
            declareQueue(waitingQueue);
            rabbitAdmin.declareExchange(exchange);
            rabbitAdmin.declareExchange(exchangeDlq);
            bindings.stream()
                .forEach(b -> rabbitAdmin.declareBinding(b));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private void declareQueue(Queue queue) {
        try {
            rabbitAdmin.declareQueue(queue);
        } catch (AmqpIOException ex) {
            logger.info("Starting the deployment of queue " + queue.getName() + " with a new configuration. Reason of Deploy: " + ex.getCause().getCause());
            new AmqpRabbitDeployer(rabbitAdmin, rabbitTemplate)
                    .deployQueueWithNewConfiguration(queue);
        }
    }

    private List<Binding> doBindings(Exchange exchange, Exchange exchangeDlq, Queue queue, Queue dlqQueue, Queue waitingQueue) {
        final Binding bindingExchangeToQueue = BindingBuilder.bind(queue).to(exchange).with(AmqpExchangeFactory.retrieveExchangeRoutingKey(queue.getName())).noargs();
        final Binding bindingExchangeToDlqQueue = BindingBuilder.bind(dlqQueue).to(exchangeDlq).with(AmqpExchangeFactory.retrieveExchangeDlqRoutingKey(queue.getName())).noargs();
        final Binding bindingExchangeToWaitingQueue = BindingBuilder.bind(waitingQueue).to(exchange).with(AmqpExchangeFactory.retrieveExchangeWaitingRoutingKey(queue.getName())).noargs();
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
        switch (retrieveAmqpExchange().amqpExchangeType()){
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

    private AmqpQueue retrieveAmqpQueue(){
        return amqpProducerMetadata.amqpQueue();
    }

    private AmqpExchange retrieveAmqpExchange(){
        return amqpProducerMetadata.amqpExchange();
    }

    private void populateAmqpProducerMetadata() {
        amqpProducerMetadata = this.getClass().getAnnotation(AmqpProducer.class);
    }

    public abstract void beforeSend(final T message);
    public abstract void afterSend(final T message);
}
