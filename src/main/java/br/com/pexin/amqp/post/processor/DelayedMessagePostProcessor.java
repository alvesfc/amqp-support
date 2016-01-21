package br.com.pexin.amqp.post.processor;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

public class DelayedMessagePostProcessor implements MessagePostProcessor {

	private final long delayTime;
	
	public DelayedMessagePostProcessor(long delayTime) {
		this.delayTime = delayTime;
	}

	@Override
	public Message postProcessMessage(Message message) throws AmqpException {
		message.getMessageProperties().setExpiration(String.valueOf(delayTime));
		return message;
	}

	@Override
	public String toString() {
		return "DelayedMessagePostProcessor [delayTime=" + delayTime + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (delayTime ^ (delayTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DelayedMessagePostProcessor other = (DelayedMessagePostProcessor) obj;
		if (delayTime != other.delayTime)
			return false;
		return true;
	}
}
