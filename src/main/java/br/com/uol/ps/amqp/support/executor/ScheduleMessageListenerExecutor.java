/**
 * 
 */
package br.com.uol.ps.amqp.support.executor;

import org.springframework.core.task.SimpleAsyncTaskExecutor;


public class ScheduleMessageListenerExecutor extends SimpleAsyncTaskExecutor {
	
	private static final long serialVersionUID = -3627458117408700147L;
	
	private long retryInterval;

	public ScheduleMessageListenerExecutor(long retryInterval) {
		this.retryInterval = retryInterval;
	}

	@Override
	public void execute(Runnable task) {
		super.execute(new ScheduleMessageListenerTask(task, retryInterval));
	}

	@Override
	public void execute(final Runnable task, long startTimeout) {
		super.execute(new ScheduleMessageListenerTask(task, retryInterval), startTimeout);
	}
	
}
