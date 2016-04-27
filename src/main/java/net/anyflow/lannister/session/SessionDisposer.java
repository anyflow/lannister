package net.anyflow.lannister.session;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.anyflow.lannister.messagehandler.MessageFactory;

public class SessionDisposer {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionDisposer.class);

	private final ChannelHandlerContext ctx;
	private final String clientId;
	private final Message will;
	private final MessageSender messageSender;

	public SessionDisposer(ChannelHandlerContext ctx, String clientId, Message will, MessageSender messageSender) {
		this.ctx = ctx;
		this.clientId = clientId;
		this.will = will;
		this.messageSender = messageSender;
	}

	public void dispose(boolean sendWill) {
		// TODO after send will, what about ack message?(when Will QoS is 1,2)
		// TODO if will should receive ack before disconnect, persistence should
		// be performed.
		if (sendWill && will != null) { // [MQTT-3.1.2-10]
			messageSender.send(MessageFactory.publish(will)).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.channel().disconnect().addListener(ChannelFutureListener.CLOSE);
					logger.debug("Session disposed. [clientId={}/channelId={}]", clientId, ctx.channel().id());
				}
			});
		}
		else {
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE);
			logger.debug("Session disposed. [clientId={}/channelId={}]", clientId, ctx.channel().id());
		}
	}
}