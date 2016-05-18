package net.anyflow.lannister.server;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import net.anyflow.lannister.Statistics;
import net.anyflow.lannister.Statistics.Criterion;

public class ByteCounterCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ByteCounterCodec.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		Statistics.SELF.add(Criterion.BYTE_SENT, msg.retain().readableBytes());
		Statistics.SELF.add(Criterion.MESSAGES_SENT, 1);

		out.add(msg);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		Statistics.SELF.add(Criterion.BYTE_RECEIVED, msg.retain().readableBytes());
		Statistics.SELF.add(Criterion.MESSAGES_RECEIVED, 1);

		out.add(msg);
	}
}