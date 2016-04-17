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

public class MqttSubscribeMessageHandler extends SimpleChannelInboundHandler<MqttSubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttSubscribeMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttSubscribeMessage msg) throws Exception {
		logger.debug(msg.toString());

		Session session = SessionNexus.SELF.getByChannelId(ctx.channel().id().toString());

		List<MqttTopicSubscription> topics = msg.payload().topicSubscriptions();

		List<Integer> grantedQoss = Lists.newArrayList();

		for (MqttTopicSubscription item : topics) {
			ITopic<MessageObject> topic = TopicNexus.SELF.get(item.topicName());

			session.topics().put(topic.getName(), item.qualityOfService());

			topic.addMessageListener(session);

			grantedQoss.add(item.qualityOfService().value());
		}

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false,
				2 + grantedQoss.size());

		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(session.nextMessageId());
		MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoss);
		ctx.channel().writeAndFlush(new MqttSubAckMessage(fixedHeader, variableHeader, payload));

		logger.debug("MqttSubscribeMessageHandler execution finished.");
	}
}
