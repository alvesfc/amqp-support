package br.com.uol.ps.amqp.support.annotation;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação que define a configuração de um Produtor.
 * Created by rafaelfirmino on 20/01/16.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface AmqpProducer {

    /**
     * Determina em milissegundos o delay para envio da mensagem para uma determinada fila.
     *
     * @return {@link int} com o delay em milissegundos.
     */
    int delayTimeInMillis() default 0;

    /**
     * Determina o modo de entrga da mensagem.
     * Caso não seja informado , será NON_PERSISTENT
     *
     * @return {@link MessageDeliveryMode} com o modo de entrga.
     */
    MessageDeliveryMode deliveryMode() default MessageDeliveryMode.NON_PERSISTENT;

    /**
     * Fila que será enviada a mensagem.
     *
     * @return {@link AmqpQueue} com a configuração da fila.
     */
    AmqpQueue amqpQueue();

    /**
     * Exchange que será utilizado para envio da mensagem.
     *
     * @return {@link AmqpExchange} com a configuração do Exchange.
     */
    AmqpExchange amqpExchange() default @AmqpExchange;
}
