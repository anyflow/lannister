package net.anyflow.menton.general;


/**
 * @author hyunjeong.park
 *
 */
public interface TaskCompletionInformer {

	/**
	 * @param taskCompletionListener
	 */
	void register(TaskCompletionListener taskCompletionListener);

	/**
	 * @param taskCompletionListener
	 */
	void deregister(TaskCompletionListener taskCompletionListener);

	/**
	 * inform task completion event to all registered object.
	 */
	void inform();
}