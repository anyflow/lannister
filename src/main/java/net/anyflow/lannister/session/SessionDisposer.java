package net.anyflow.lannister.session;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.topic.Topic;

public class SessionDisposer {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionDisposer.class);

	private final ChannelHandlerContext ctx;
	private final String clientId;
	private final Message will;

	protected SessionDisposer(ChannelHandlerContext ctx, String clientId, Message will) {
		this.ctx = ctx;
		this.clientId = clientId;
		this.will = will;
	}

	protected void dispose(boolean sendWill) {
		// TODO after send will, what about ack message?(when Will QoS is 1,2)
		// TODO if will should receive ack before disconnect, persistence should
		// be performed.
		if (sendWill && will != null) { // [MQTT-3.1.2-10]
			Topic.NEXUS.get(will.topicName()).publish(clientId, will);
		}
		else {
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE);
			logger.debug("Session disposed. [clientId={}/channelId={}]", clientId, ctx.channel().id());
		}
	}
}