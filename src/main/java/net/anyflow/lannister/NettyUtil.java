package net.anyflow.lannister;

import io.netty.buffer.ByteBuf;

public class NettyUtil {
	public static byte[] copy(ByteBuf buffer) {
		byte[] bytes = new byte[buffer.readableBytes()];

		int readerIndex = buffer.readerIndex();
		buffer.getBytes(readerIndex, bytes);

		return bytes;
	}
}
