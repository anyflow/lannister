package net.anyflow.lannister.plugin;

public interface PublishEventListener extends Plugin {

	public boolean beforePublish(PublishEventArgs args);
}