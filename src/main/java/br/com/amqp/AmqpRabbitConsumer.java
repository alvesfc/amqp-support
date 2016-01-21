package br.com.amqp;

import br.com.amqp.annotation.AmqpQueue;
import br.com.amqp.annotation.AmqpRetryPolicy;
import br.com.amqp.factory.AmqpExchangeFactory;
import br.com.amqp.annotation.AmqpConsumer;
import br.com.amqp.executor.ScheduleMessageListenerExecutor;
import com.rabbitmq.client.Channel;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Created by rafaelfirmino on 20/01/16.
 */
public abstract class AmqpRabbitConsumer<T, R> extends SimpleMessageListenerContainer implements ChannelAwareMessageListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private AmqpConsumer amqpConsumerMetadata;

    private static final int TASK_EXECUTOR_RETRY_INTERVAL = 30000;
    private static final int RECOVERY_INTERVAL = 1000;
    private static final int RECEIVE_TIMEOUT = 1000;
    private static final int SHUTDOWN_TIMEOUT = 10000;
    private static final int PREFETCH_COUNT = 1;

    @PostConstruct
    private void setUp(){
        try{

            populateAmqpConsumerMetadata();

            setTaskExecutor(new ScheduleMessageListenerExecutor(TASK_EXECUTOR_RETRY_INTERVAL));
            setConnectionFactory(rabbitTemplate.getConnectionFactory());
            setChannelTransacted(rabbitTemplate.isChannelTransacted());
            setQueueNames(retrieveQueueName());
            setAcknowledgeMode(AcknowledgeMode.AUTO);
            setConcurrentConsumers(retrieveConcurrentConsumers());
            setRecoveryInterval(RECOVERY_INTERVAL);
            setReceiveTimeout(RECEIVE_TIMEOUT);
            setShutdownTimeout(SHUTDOWN_TIMEOUT);
            setPrefetchCount(PREFETCH_COUNT);
            setMessageListener(this);
            setAdviceChain(new Advice[] {
                    workMessagesRetryInterceptor()
            });
            setErrorHandler(new ErrorHandler() {
                @Override
                public void handleError(Throwable t) {
                    logger.error(t);
                }
            });

        }catch (Exception e){
            throw new IllegalStateException(e);
        }
    }

    @Override
    public final void onMessage(Message message, Channel channel) throws Exception {

        try {
            final T messageConverted = convertMessage(message);
            final R response = this.onMessage(messageConverted);
            sendReplyMessage(message, channel, response);
        }catch (ClassCastException | MessageConversionException | IllegalArgumentException | NullPointerException e){
            logger.warn("Message rejected from special excetions. " + message);
        }catch (Exception e){
            logger.warn("Message with error in consumer. Throw AmqpException to retry. " + message);
            throw new AmqpException(e);
        }
    }

    private int retrieveConcurrentConsumers() {
        return amqpConsumerMetadata.concurrentConsumers();
    }

    private String retrieveQueueName() {
        return retrieveAmqpQueue().name();
    }

    private AmqpQueue retrieveAmqpQueue(){
        return amqpConsumerMetadata.amqpQueue();
    }

    private AmqpRetryPolicy retrieveAmqpRetryPolicy(){
        return amqpConsumerMetadata.amqpRetryPolicy();
    }

    private Address getReplyToAddress(final Message request) {
        return request.getMessageProperties().getReplyToAddress();
    }

    private Message toMessage(final R object) {
        return rabbitTemplate.getMessageConverter().toMessage(object, new MessageProperties());
    }

    private void sendReplyMessage(final Message message, final Channel channel, final R response) throws IOException {
        Address replyToAddress = getReplyToAddress(message);
        if (replyToAddress != null && response != null) {
            Message responseMessage = toMessage(response);
            try {
                publishReply(channel, replyToAddress, responseMessage);
            } catch (IOException ex) {
                final String error = String.format("error=\"Unexpected Error when publishing reply\" cause=%s message=%s", message, ex.getMessage());
                logger.error(error);
            }
        }
    }

    private void publishReply(final Channel channel, final Address replyToAddress, final Message responseMessage) throws IOException {
        channel.basicPublish(replyToAddress.getExchangeName(),
                replyToAddress.getRoutingKey(),
                new DefaultMessagePropertiesConverter().fromMessageProperties(responseMessage.getMessageProperties(), "UTF-8"),
                responseMessage.getBody());
    }

    private T convertMessage(Message message) {
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

    private void populateAmqpConsumerMetadata() {
        amqpConsumerMetadata = this.getClass().getAnnotation(AmqpConsumer.class);
    }

    private RetryOperationsInterceptor workMessagesRetryInterceptor() {

        final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(retrieveAmqpRetryPolicy().maxRetryAttemps());

        final ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(retrieveAmqpRetryPolicy().timeToRetryInMillis());
        exponentialBackOffPolicy.setMaxInterval(retrieveAmqpRetryPolicy().timeToRetryInMillis());
        exponentialBackOffPolicy.setMultiplier(1);

        final RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(simpleRetryPolicy);
        template.setBackOffPolicy(exponentialBackOffPolicy);

        final String exchangeName = AmqpExchangeFactory.retrieveDlqExchangeName(retrieveQueueName());
        final String exchangeDlqRoutingKey = AmqpExchangeFactory.retrieveExchangeDlqRoutingKey(retrieveQueueName());

        return RetryInterceptorBuilder
                .stateless()
                .retryOperations(template)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, exchangeName, exchangeDlqRoutingKey))
                .build();
    }

    public abstract R onMessage(T message);
}
