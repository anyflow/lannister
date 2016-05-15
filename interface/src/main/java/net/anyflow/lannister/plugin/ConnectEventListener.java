package net.anyflow.lannister.plugin;

import net.anyflow.lannister.entity.ConnectEventArgs;

public interface ConnectEventListener extends Plugin {

	public void connectHandled(ConnectEventArgs args);
}
