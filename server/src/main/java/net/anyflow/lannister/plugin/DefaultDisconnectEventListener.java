package net.anyflow.lannister.plugin;

public class DefaultDisconnectEventListener implements DisconnectEventListener {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(DefaultDisconnectEventListener.class);

	@Override
	public Plugin clone() {
		return this;
	}

	@Override
	public void disconnected(DisconnectEventArgs args) {
		logger.debug("DefaultDisconnectEventListener.disconnected() called [{}]", args.log());
	}
}