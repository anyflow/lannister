package net.anyflow.lannister.pluginexample;

import net.anyflow.lannister.plugin.DisconnectEventArgs;
import net.anyflow.lannister.plugin.DisconnectEventListener;
import net.anyflow.lannister.plugin.Plugin;

public class ExampleDisconnectEventListener implements DisconnectEventListener {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ExampleDisconnectEventListener.class);

	@Override
	public Plugin clone() {
		return new ExampleDisconnectEventListener();
	}

	@Override
	public void disconnected(DisconnectEventArgs args) {
		logger.debug("ExampleDisconnectEventListener.disconnected() called [{}]", args.log());
	}
}