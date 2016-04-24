package net.anyflow.lannister.session;

import java.util.Map;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.anyflow.lannister.messagehandler.MessageFactory;

public class SessionDisposer {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionDisposer.class);

	private final ChannelHandlerContext ctx;
	private final Map<String, Topic> topics;
	private final String clientId;
	private final Message will;
	private final MessageSender messageSender;

	public SessionDisposer(ChannelHandlerContext ctx, String clientId, Map<String, Topic> topics, Message will,
			MessageSender messageSender) {
		this.ctx = ctx;
		this.topics = topics;
		this.clientId = clientId;
		this.will = will;
		this.messageSender = messageSender;
	}

	public void dispose(boolean sendWill) {
		ctx.disconnect().addListener(ChannelFutureListener.CLOSE);

		for (String topicName : topics.keySet()) {
			Repository.SELF.broadcaster(topicName).removeMessageListener(topics.get(topicName).registrationId());
		}

		if (will != null) { // [MQTT-3.1.2-10]
			messageSender.send(MessageFactory.publish(will));
		}

		logger.debug("Session disposed. [clientId={}/channelId={}]", clientId, ctx.channel().id());

		for (Topic topic : topics.values()) {
			topic.setRetainedMessage(null); // [MQTT-3.1.2.7]
		}
	}
}