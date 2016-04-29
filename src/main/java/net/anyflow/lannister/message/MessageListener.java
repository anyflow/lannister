package net.anyflow.lannister.message;

import java.util.List;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.EventExecutor;
import net.anyflow.lannister.admin.command.MessageFilter;
import net.anyflow.lannister.admin.command.SessionsFilter;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.Synchronizer;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscription;

public class MessageListener {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageListener.class);

	private final static List<MessageFilter> FILTERS = Lists.newArrayList(new SessionsFilter());

	private static final int MAX_MESSAGE_ID_NUM = 0xffff;
	private static final int MIN_MESSAGE_ID_NUM = 1;

	private final Session session;
	private final Synchronizer synchronizer;
	private final MessageSender messageSender;
	private final EventExecutor eventExecutor;

	private int currentMessageId;

	public MessageListener(Session session, Synchronizer synchronizer, MessageSender messageSender,
			int currentMessageId, EventExecutor eventExecutor) {
		this.session = session;
		this.synchronizer = synchronizer;
		this.messageSender = messageSender;
		this.currentMessageId = currentMessageId;
		this.eventExecutor = eventExecutor;
	}

	public void onPublish(Topic topic, Message message) {
		eventExecutor.submit(new Runnable() {
			@Override
			public void run() {
				logger.debug("event arrived : [clientId:{}/message:{}]", session.clientId(), message.toString());

				// TODO what if returned topicSubscriptions are multiple?

				TopicSubscription[] tss = session.matches(message.topicName());

				if (tss == null || tss.length <= 0) {
					logger.error("Topic Subscription could not be null [clientId={}, topicName={}]", session.clientId(),
							message.topicName());
					return;
				}

				if (session.isConnected() == false) { return; }

				executefilters(message);

				final int originalMessageId = message.id();
				message.setId(nextMessageId());
				message.setRetain(false);// [MQTT-3.3.1-9]

				if (message.qos() != MqttQoS.AT_MOST_ONCE) {
					topic.subscribers().get(session.clientId()).addSentMessageStatus(message.id(), originalMessageId,
							SenderTargetStatus.TO_PUB);
				}

				messageSender.send(MessageFactory.publish(message, false)).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (message.qos() == MqttQoS.AT_MOST_ONCE) { return; }

						topic.subscribers().get(session.clientId()).setSentMessageStatus(message.id(),
								message.qos().equals(MqttQoS.EXACTLY_ONCE) ? SenderTargetStatus.TO_REL
										: SenderTargetStatus.NOTHING);
					}
				});
			}
		});
	}

	private static void executefilters(Message message) {
		for (MessageFilter filter : FILTERS) {
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
