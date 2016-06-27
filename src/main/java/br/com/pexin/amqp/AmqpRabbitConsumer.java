package br.com.pexin.amqp;

import br.com.pexin.amqp.annotation.AmqpConsumer;
import br.com.pexin.amqp.annotation.AmqpRetryPolicy;
import br.com.pexin.amqp.executor.ScheduleMessageListenerExecutor;
import br.com.pexin.amqp.factory.AmqpExchangeFactory;
import br.com.pexin.amqp.factory.AmqpQueueFactory;
import br.com.pexin.amqp.log.FluentLogger;
import com.rabbitmq.client.Channel;
import org.aopalliance.aop.Advice;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by rafaelfirmino on 20/01/16.
 */
public abstract class AmqpRabbitConsumer<T, R> extends SimpleMessageListenerContainer
        implements ChannelAwareMessageListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;

    private AmqpConsumer amqpConsumerMetadata;
    private FluentLogger fluentLogger = new FluentLogger(LogFactory.getLog(getClass()));

    private static final int TASK_EXECUTOR_RETRY_INTERVAL = 30000;
    private static final int RECOVERY_INTERVAL = 1000;
    private static final int RECEIVE_TIMEOUT = 1000;
    private static final int SHUTDOWN_TIMEOUT = 10000;
    private static final int PREFETCH_COUNT = 1;

    @PostConstruct
    private void setUp() {
        try {

            populateAmqpConsumerMetadata();
            createQueueIfNecessary();

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
            setErrorHandler(t -> fluentLogger
                    .key("setUp")
                    .value("errorHandler")
                    .key("cause")
                    .value(t.getMessage())
                    .logError()
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public final void onMessage(Message message, Channel channel) throws Exception {

        try {
            final T messageConverted = convertMessage(message);
            final R response = this.onMessage(messageConverted);
            sendReplyMessage(message, channel, response);
        } catch (ClassCastException | MessageConversionException | IllegalArgumentException | NullPointerException e) {
            fluentLogger
                    .key("method")
                    .value("onMessage")
                    .key("cause")
                    .value("Message rejected from special excetions. " + message)
                    .logWarn();
        } catch (Exception e) {
            fluentLogger
                    .key("method")
                    .value("onMessage")
                    .key("cause")
                    .value("Message with error in consumer. Throw AmqpException to retry. " + message)
                    .logWarn();
            throw new AmqpException(e);
        }
    }

    private int retrieveConcurrentConsumers() {
        return amqpConsumerMetadata.concurrentConsumers();
    }

    private String retrieveQueueName() {
        return AmqpQueueFactory.retrieveQueueName(amqpConsumerMetadata.amqpQueue());
    }

    private AmqpRetryPolicy retrieveAmqpRetryPolicy() {
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
                fluentLogger
                        .key("method")
                        .value("sendReplyMessage")
                        .key("message")
                        .value(message)
                        .key("cause")
                        .value(ex.getMessage())
                        .logError();
            }
        }
    }

    private void publishReply(final Channel channel, final Address replyToAddress, final Message responseMessage)
            throws IOException {
        channel.basicPublish(replyToAddress.getExchangeName(),
                replyToAddress.getRoutingKey(),
                new DefaultMessagePropertiesConverter()
                        .fromMessageProperties(responseMessage.getMessageProperties(), "UTF-8"),
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

    private void createQueueIfNecessary() {
        if (!hasQueueDeployed(retrieveQueueName())) {
            final Queue queue = new Queue(retrieveQueueName());
            try {
                rabbitAdmin.declareQueue(queue);
            } catch (AmqpIOException ex) {
                fluentLogger
                        .key("method")
                        .value("createQueueIfNecessary")
                        .key("cause")
                        .value("Error in creating new queue on consumer..." + ex.getMessage())
                        .logWarn();
            }
        }
    }

    private boolean hasQueueDeployed(String queueName) {
        final Properties properties = rabbitAdmin.getQueueProperties(queueName);
        return properties != null;
    }

    public abstract R onMessage(T message);
}
