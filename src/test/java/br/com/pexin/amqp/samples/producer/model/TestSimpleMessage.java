package br.com.pexin.amqp.samples.producer.model;

/**
 * Created by rafaelfirmino on 20/01/16.
 */
public class TestSimpleMessage {

    private int id;

    public TestSimpleMessage(){}

    public TestSimpleMessage(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Teste{" +
                "id=" + id +
                '}';
    }
}
