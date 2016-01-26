package br.com.pexin.amqp.annotation;

/**
 * Created by rafaelfirmino on 20/01/16.
 */
public @interface AmqpExchange {
    public AmqpExchangeType amqpExchangeType() default AmqpExchangeType.DIRECT;
    public boolean durable() default true;
    public boolean autoDelete() default false;
}
