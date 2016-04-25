package net.anyflow.lannister.session;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.EventExecutor;
import net.anyflow.lannister.admin.command.MessageFilter;
import net.anyflow.lannister.admin.command.SessionsFilter;
import net.anyflow.lannister.messagehandler.MessageFactory;

public class MessageListener {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageListener.class);

	private static final int MAX_MESSAGE_ID_NUM = 0xffff;
	private static final int MIN_MESSAGE_ID_NUM = 1;

	private final Session session;
	private final Map<String, Topic> topics;
	private final Map<Integer, Message> messages;
	private final Synchronizer synchronizer;
	private final List<MessageFilter> filters;
	private final MessageSender messageSender;
	private final EventExecutor eventExecutor;

	private int currentMessageId;

	public MessageListener(Session session, Map<String, Topic> topics, Map<Integer, Message> messages,
			Synchronizer synchronizer, MessageSender messageSender, int currentMessageId, EventExecutor eventExecutor) {
		this.session = session;
		this.topics = topics;
		this.messages = messages;
		this.synchronizer = synchronizer;
		this.messageSender = messageSender;
		this.currentMessageId = currentMessageId;
		this.eventExecutor = eventExecutor;

		this.filters = Lists.newArrayList(new SessionsFilter());
	}

	public void onMessage(com.hazelcast.core.Message<Message> rawMessage) {
		final Message received = rawMessage.getMessageObject();

		eventExecutor.submit(new Runnable() {
			@Override
			public void run() {
				logger.debug("event arrived : [clientId:{}/message:{}]", session.clientId(), received.toString());

				executefilters(received);

				Topic topic = topics.get(received.topicName());

				if (received.isRetain()) {
					if (received.message().length > 0) {
						topic.setRetainedMessage(
								new Message(nextMessageId(), received.topicName(), received.message(), null, true));
					}
					else {
						topic.setRetainedMessage(null); // [MQTT-3.3.1-10],[MQTT-3.3.1-11]
					}
				}
				else {
					// do nothing [MQTT-3.3.1-12]
				}

				Message toSend = new Message(nextMessageId(), received.topicName(), received.message(),
						MessageSender.adjustQoS(topic.qos(), received.qos()), false);// [MQTT-3.3.1-9]

				if (toSend.qos().value() > 0) {
					messages.put(toSend.id(), toSend);
				}

				if (session.isConnected() == false) {
					synchronizer.execute();
					return;
				}

				messageSender.send(MessageFactory.publish(toSend)).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						toSend.setSent(true);
						synchronizer.execute();
					}
				});
			}
		});
	}

	private void executefilters(Message message) {
		for (MessageFilter filter : filters) {
			filter.execute(message);
		}
	}

	private int nextMessageId() {
		currentMessageId = currentMessageId + 1;

		if (currentMessageId > MAX_MESSAGE_ID_NUM) {
			currentMessageId = MIN_MESSAGE_ID_NUM;
		}

		synchronizer.execute();

		return currentMessageId;
	}
}
