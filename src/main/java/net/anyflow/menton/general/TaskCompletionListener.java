/**
 * 
 */
package net.anyflow.menton.general;

/**
 * @author Park Hyunjeong
 */
public interface TaskCompletionListener {

	/**
	 * called after task completed
	 * 
	 * @param worker worker object that perform the task.
	 * @param furtherTaskingAvailable true if the object is able to have further task.
	 */
	void taskCompleted(Object worker, boolean furtherTaskingAvailable);
}