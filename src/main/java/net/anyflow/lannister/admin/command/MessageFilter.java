package net.anyflow.lannister.admin.command;

import net.anyflow.lannister.message.Message;

public interface MessageFilter {
	void execute(Message message);
}
