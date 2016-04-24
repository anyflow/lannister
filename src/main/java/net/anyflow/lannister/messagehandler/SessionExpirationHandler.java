package net.anyflow.lannister.messagehandler;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.session.LiveSessions;
import net.anyflow.lannister.session.Session;

public class SessionExpirationHandler extends ChannelInboundHandlerAdapter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionExpirationHandler.class);

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

		int interval = Settings.SELF.getInt("lannister.sessionExpirationHandlerExecutionIntervalInSecond", 0);

		ctx.executor().scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				Collection<Session> sessions = LiveSessions.SELF.clientIdMap().values();
				List<Session> disposes = Lists.newArrayList();

				for (Session item : sessions) {
					if (item.isExpired() == false) {
						continue;
					}

					disposes.add(item);
				}

				for (Session item : disposes) {
					LiveSessions.SELF.dispose(item, true); // [MQTT-3.1.2-24]
				}

				logger.debug("SessionExpirationHandler executed : [dispose count={}]", disposes.size());
			}

		}, interval, interval, TimeUnit.SECONDS);
	}
}