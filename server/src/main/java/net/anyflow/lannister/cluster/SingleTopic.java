package net.anyflow.lannister.cluster;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.monitor.LocalTopicStats;

import io.netty.util.concurrent.GlobalEventExecutor;

public class SingleTopic<E> implements ITopic<E> {

	private final String name;
	private final Map<String, MessageListener<E>> messageListeners;

	public SingleTopic(String name) {
		this.name = name;
		this.messageListeners = Maps.newConcurrentMap();
	}

	@Override
	public void destroy() {
		// DO NOTHING
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void publish(E message) {
		final Date now = new Date();

		GlobalEventExecutor.INSTANCE.execute(() -> {
			Message<E> msg = new Message<E>(name, message, now.getTime(), null);
			messageListeners.values().forEach(c -> c.onMessage(msg));
		});
	}

	@Override
	public String addMessageListener(MessageListener<E> listener) {
		UUID ret = UUID.randomUUID();

		messageListeners.put(ret.toString(), listener);

		return ret.toString();
	}

	@Override
	public boolean removeMessageListener(String registrationId) {
		return messageListeners.remove(registrationId) != null;
	}

	@Override
	public String getPartitionKey() {
		throw new Error("The method should not be called");
	}

	@Override
	public String getServiceName() {
		throw new Error("The method should not be called");
	}

	@Override
	public LocalTopicStats getLocalTopicStats() {
		throw new Error("The method should not be called");
	}
}
