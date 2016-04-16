package net.anyflow.lannister.session;

import java.util.Date;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import io.netty.channel.ChannelHandlerContext;

public class Session implements MessageListener<String> {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	private String clientId;
	private ChannelHandlerContext ctx;
	private Date createTime;

	public Session(ChannelHandlerContext ctx, String clientId) {
		this.ctx = ctx;
		this.clientId = clientId;
		this.createTime = new Date();
	}

	public String clientId() {
		return clientId;
	}

	public ChannelHandlerContext ctx() {
		return ctx;
	}

	public Date createTime() {
		return createTime;
	}

	@Override
	public void onMessage(Message<String> message) {
	}
}
