package br.com.pexin.amqp.executor;

public class ScheduleMessageListenerTask implements Runnable {


	private Runnable delegateTask;

	private long retryInterval;

	public ScheduleMessageListenerTask(Runnable delegateTask, long retryInterval) {
		this.delegateTask = delegateTask;
		this.retryInterval = retryInterval;
	}

	@Override
	public void run() {
		boolean error;

		do {
			try {
				error = false;
				delegateTask.run();
			} catch (Exception e) {
				error = true;
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Unrecoverable interruption on consumer restart");
				}
			}
		} while (error);
	}
}
