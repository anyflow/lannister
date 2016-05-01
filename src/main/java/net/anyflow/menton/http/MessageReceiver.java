/**
 * 
 */
package net.anyflow.menton.http;

/**
 * @author anyflow
 */
public interface MessageReceiver {

	/**
	 * Will be called on message received.
	 * 
	 * @param request
	 * @param response
	 */
	void messageReceived(HttpRequest request, HttpResponse response);
}