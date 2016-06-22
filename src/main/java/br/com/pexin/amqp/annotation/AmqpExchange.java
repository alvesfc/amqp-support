package br.com.pexin.amqp.annotation;

/**
 * Anotação que define a configuração de um Exchange.
 * Created by rafaelfirmino on 20/01/16.
 */
public @interface AmqpExchange {
    /**
     * Tipo de exchange que será criado.
     * Caso não seja informado , será criado do tipo DIRECT.
     *
     * @return {@link AmqpExchangeType} com o tipo de exchange.
     */
    AmqpExchangeType amqpExchangeType() default AmqpExchangeType.DIRECT;

    /**
     * Determina se o exchange será durável ou não.
     * Caso não seja informado o exchange será do tipo durável.
     *
     * @return {@link boolean} com o valor true ou false.
     */
    boolean durable() default true;

    /**
     * Determina se será removido automaticamente ao iniciar o cantainer.
     * Caso não seja informado o exchange não será removido automaticamente ao iniciar o cantainer.
     *
     * @return {@link boolean} com o valor true ou false.
     */
    boolean autoDelete() default false;
}
