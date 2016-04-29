package net.anyflow.lannister.packetreceiver;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.session.Session;

public class SessionExpirator extends ChannelInboundHandlerAdapter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionExpirator.class);

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

		int interval = Settings.SELF.getInt("lannister.sessionExpirationHandlerExecutionIntervalSeconds", 0);

		ctx.executor().scheduleWithFixedDelay(() -> {
			Collection<Session> sessions = Session.NEXUS.lives().values();
			List<Session> disposes = Lists.newArrayList();

			sessions.stream().filter(s -> s.isExpired()).forEach(s -> disposes.add(s));

			disposes.stream().forEach(s -> s.dispose(true)); // [MQTT-3.1.2-24]

			logger.debug("SessionExpirationHandler executed : [dispose count={}]", disposes.size());

		} , interval, interval, TimeUnit.SECONDS);
	}
}