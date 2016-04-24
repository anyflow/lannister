package net.anyflow.lannister.session;

import java.util.Map;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

public class SessionDisposer {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionDisposer.class);

	private final ChannelHandlerContext ctx;
	private final Map<String, Topic> topics;
	private final String clientId;

	public SessionDisposer(ChannelHandlerContext ctx, Map<String, Topic> topics, String clientId) {
		this.ctx = ctx;
		this.topics = topics;
		this.clientId = clientId;
	}

	public void dispose(boolean sendWill) {
		ctx.disconnect().addListener(ChannelFutureListener.CLOSE);

		for (String topicName : topics.keySet()) {
			Repository.SELF.broadcaster(topicName).removeMessageListener(topics.get(topicName).registrationId());
		}

		// TODO send Will

		logger.debug("Session disposed. [clientId={}/channelId={}]", clientId, ctx.channel().id());

		for (Topic topic : topics.values()) {
			topic.setRetainedMessage(null); // [MQTT-3.1.2.7]
		}
	}
}
