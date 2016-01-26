package br.com.pexin.amqp.annotation;

/**
 * Created by rafaelfirmino on 20/01/16.
 */
public @interface AmqpQueue {

    public String name();
    public boolean durable() default true;
    public boolean exclusive() default false;
    public boolean autoDelete() default false;
}
