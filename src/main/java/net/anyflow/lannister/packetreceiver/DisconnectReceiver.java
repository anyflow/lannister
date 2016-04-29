package net.anyflow.lannister.packetreceiver;

import net.anyflow.lannister.session.Session;

public class DisconnectReceiver {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DisconnectReceiver.class);

	public static DisconnectReceiver SHARED = new DisconnectReceiver();

	private DisconnectReceiver() {
	}

	protected void handle(Session session) {
		session.dispose(false); // [MQTT-3.14.4-1],[MQTT-3.14.4-2],[MQTT-3.14.4-3]
	}
}