package net.anyflow.lannister.plugin;

import net.anyflow.lannister.message.IMessage;

public interface PublishEventArgs {
	IMessage message();

	default String log() {
		return (new StringBuilder()).append(message().log()).toString();
	}
}
