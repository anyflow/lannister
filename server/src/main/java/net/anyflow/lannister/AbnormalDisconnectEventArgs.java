package net.anyflow.lannister;

import net.anyflow.lannister.plugin.DisconnectEventArgs;

public class AbnormalDisconnectEventArgs implements DisconnectEventArgs {
	@Override
	public String clientId() {
		return null;
	}

	@Override
	public Boolean cleanSession() {
		return null;
	}

	@Override
	public Boolean byDisconnectMessage() {
		return false;
	}
}
