package net.anyflow.lannister.messagehandler;

import com.hazelcast.core.ITopic;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.session.TopicNexus;

public class MqttPublishMessageHandler extends SimpleChannelInboundHandler<MqttPublishMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPublishMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPublishMessage msg) throws Exception {
		logger.debug(msg.toString());

		ITopic<String> topic = TopicNexus.SELF.get(msg.variableHeader().topicName());

		topic.publish(msg.payload().toString(CharsetUtil.UTF_8));

		// TODO send pubAck
	}
}