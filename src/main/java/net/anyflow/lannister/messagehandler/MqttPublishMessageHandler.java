package net.anyflow.lannister.messagehandler;

import com.hazelcast.core.ITopic;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.NettyUtil;
import net.anyflow.lannister.session.LiveSessions;
import net.anyflow.lannister.session.MessageObject;
import net.anyflow.lannister.session.Repository;
import net.anyflow.lannister.session.Session;

public class MqttPublishMessageHandler extends SimpleChannelInboundHandler<MqttPublishMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPublishMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPublishMessage msg) throws Exception {
		logger.debug(msg.toString());

		Session session = LiveSessions.SELF.getByChannelId(ctx.channel().id());
		if (session == null) {
			logger.error("session does not exist. {}", ctx.channel().id().toString());
			// TODO handing null session
			return;
		}

		ITopic<MessageObject> topic = Repository.SELF.topic(msg.variableHeader().topicName());

		topic.publish(new MessageObject(msg.variableHeader().messageId(), msg.variableHeader().topicName(),
				NettyUtil.copy(msg.payload())));

		// TODO QoS leveling
		switch (msg.fixedHeader().qosLevel()) {
		case AT_MOST_ONCE:
			return;

		case AT_LEAST_ONCE:
			MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_LEAST_ONCE,
					false, 2);

			MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader
					.from(msg.variableHeader().messageId());
			ctx.channel().writeAndFlush(new MqttPubAckMessage(fixedHeader, variableHeader));
			return;

		default:
			throw new IllegalArgumentException("only qos 0, 1 supported");
		}
	}
}