package net.anyflow.lannister.packetreceiver;

import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.session.Session;

public class PingReqReceiver {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PingReqReceiver.class);

	public static PingReqReceiver SHARED = new PingReqReceiver();

	private PingReqReceiver() {
	}

	protected void handle(Session session) {
		session.send(MessageFactory.pingresp()); // [MQTT-3.12.4-1]
	}
}