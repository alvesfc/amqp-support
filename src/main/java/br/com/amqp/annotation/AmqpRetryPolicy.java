package br.com.amqp.annotation;

/**
 * Created by rafaelfirmino on 20/01/16.
 */
public @interface AmqpRetryPolicy {
    public int maxRetryAttemps() default 5;
    public int timeToRetryInMillis() default 600000;
}
