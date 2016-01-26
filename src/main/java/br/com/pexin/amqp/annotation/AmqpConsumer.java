package br.com.pexin.amqp.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by rafaelfirmino on 20/01/16.
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface AmqpConsumer {
    public AmqpQueue amqpQueue();
    public AmqpQueue amqpDlqQueue();
    public AmqpRetryPolicy amqpRetryPolicy();
    public int concurrentConsumers() default 1;
}
