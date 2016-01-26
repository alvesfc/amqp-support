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
public @interface AmqpProducer {
    public int delayTimeInMillis() default 0;
    public AmqpQueue amqpQueue();
    public AmqpExchange amqpExchange() default @AmqpExchange;
}
