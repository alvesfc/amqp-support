package br.com.pexin.amqp.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação que define a configuração de um Consumidor.
 * Created by rafaelfirmino on 20/01/16.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface AmqpConsumer {

    /**
     * Determina a fila que será utilizada pelo consumidor.
     *
     * @return {@link AmqpQueue} com a configuração da fila.
     */
    AmqpQueue amqpQueue();

    /**
     * Determina a fila que será utilizada como DLQ (dead-letter queue).
     *
     * @return {@link AmqpQueue} com a configuração da fila.
     */
    AmqpQueue amqpDlqQueue();

    /**
     * Determina a política de retentativa que será utilizada.
     *
     * @return {@link AmqpRetryPolicy} com a configuração da política de retentativa.
     */
    AmqpRetryPolicy amqpRetryPolicy();

    /**
     * Determina o número de consumidores concorrentes para a fila configurada.
     *
     * @return {@link int} com o número de consumidores concorrentes.
     */
    int concurrentConsumers() default 1;
}
