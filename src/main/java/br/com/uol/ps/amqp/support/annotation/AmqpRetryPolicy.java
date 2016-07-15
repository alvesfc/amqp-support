package br.com.uol.ps.amqp.support.annotation;

/**
 * Anotação que define a política de retentativas de envio da mensagem.
 * Created by rafaelfirmino on 20/01/16.
 */
public @interface AmqpRetryPolicy {

    /**
     * Define o número de retentativas de envio da mensagem
     * O Número default será de 5 retentativas , caso não seja informado.
     *
     * @return int com a quantidade de retentativas
     */
    int maxRetryAttemps() default 5;

    /**
     * Define o tempo em milissegundos entre as retentativas
     * O Número default será de 600000 milissegundos , caso não seja informado.
     *
     * @return int com o tempo em milissegundos.
     */
    int timeToRetryInMillis() default 600000;
}
