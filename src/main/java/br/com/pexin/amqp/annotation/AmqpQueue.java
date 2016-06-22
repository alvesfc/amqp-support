package br.com.pexin.amqp.annotation;

/**
 * Anotação que define a configuração da fila.
 * Created by rafaelfirmino on 20/01/16.
 */
public @interface AmqpQueue {

    /**
     * Define o nome da fila.
     *
     * @return {@link String} com o nome da fila.
     */
    String name();

    /**
     * Determina se a fila será durável ou não.
     * Caso não seja informado a fila será do tipo durável.
     *
     * @return {@link boolean} com o valor true ou false.
     */
    boolean durable() default true;

    /**
     * Determina se terá um consumidor único no container com acesso exclusivo para fila.
     * Caso não seja informado a fila não terá um consumidor único no container com acesso exclusivo.
     *
     * @return {@link boolean} com o valor true ou false.
     */
    boolean exclusive() default false;

    /**
     * Determina se será removida automaticamente ao iniciar o cantainer.
     * Caso não seja informado a fila não será removida automaticamente ao iniciar o cantainer.
     *
     * @return {@link boolean} com o valor true ou false.
     */
    boolean autoDelete() default false;
}
