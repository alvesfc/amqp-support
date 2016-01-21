package br.com.pexin.amqp.post.processor;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

import java.util.Objects;

public class DefaultMessagePostProcessor<T> implements MessagePostProcessor {

	private final T body;

	public DefaultMessagePostProcessor(T body) {
		this.body = body;
	}

	@Override
	public Message postProcessMessage(Message message) throws AmqpException {
		return message;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 79 * hash + Objects.hashCode(this.body);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		final DefaultMessagePostProcessor<T> other = (DefaultMessagePostProcessor<T>) obj;
		if (!Objects.equals(this.body, other.body)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DefaultMessagePostProcessor [body=" + body + "]";
	}
}
