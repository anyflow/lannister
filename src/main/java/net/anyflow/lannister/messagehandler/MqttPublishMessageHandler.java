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
import net.anyflow.lannister.session.MessageObject;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.SessionNexus;
import net.anyflow.lannister.session.TopicNexus;

public class MqttPublishMessageHandler extends SimpleChannelInboundHandler<MqttPublishMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPublishMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPublishMessage msg) throws Exception {
		logger.debug(msg.toString());

		ITopic<MessageObject> topic = TopicNexus.SELF.get(msg.variableHeader().topicName());

		topic.publish(new MessageObject(msg.variableHeader().messageId(), msg.variableHeader().topicName(),
				NettyUtil.copy(msg.payload())));

		// TODO QoS leveling

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false,
				2);

		Session session = SessionNexus.SELF.getByChannelId(ctx.channel().id().toString());
		if (session == null) {
			logger.error("session does not exist. {}", ctx.channel().id().toString());
			// TODO handing null session
			return;
		}

		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(session.nextMessageId());

		ctx.channel().writeAndFlush(new MqttPubAckMessage(fixedHeader, variableHeader));

		logger.debug("MqttPublishMessageHandler execution finished.");
	}
}