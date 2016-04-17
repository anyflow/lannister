package net.anyflow.lannister.messagehandler;

import java.util.List;

import com.google.common.collect.Lists;
import com.hazelcast.core.ITopic;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import net.anyflow.lannister.session.MessageObject;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.SessionNexus;
import net.anyflow.lannister.session.TopicNexus;
import net.anyflow.lannister.session.TopicRegister;

public class MqttSubscribeMessageHandler extends SimpleChannelInboundHandler<MqttSubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttSubscribeMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttSubscribeMessage msg) throws Exception {
		logger.debug(msg.toString());

		Session session = SessionNexus.SELF.getByChannelId(ctx.channel().id().toString());
		if (session == null) {
			logger.error("session does not exist. {}", ctx.channel().id().toString());
			// TODO handing null session
			return;
		}

		List<MqttTopicSubscription> topics = msg.payload().topicSubscriptions();

		List<Integer> grantedQoss = Lists.newArrayList();

		for (MqttTopicSubscription item : topics) {
			ITopic<MessageObject> topic = TopicNexus.SELF.get(item.topicName());

			String registrationId = topic.addMessageListener(session);
			session.topicRegisters().put(topic.getName(), new TopicRegister(registrationId, item.qualityOfService()));

			grantedQoss.add(item.qualityOfService().value());
		}

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_LEAST_ONCE, false,
				2 + grantedQoss.size());

		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(msg.variableHeader().messageId());
		MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoss);
		ctx.channel().writeAndFlush(new MqttSubAckMessage(fixedHeader, variableHeader, payload));
	}
}