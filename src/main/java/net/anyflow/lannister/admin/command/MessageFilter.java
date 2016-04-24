package net.anyflow.lannister.admin.command;

import net.anyflow.lannister.session.Message;

public interface MessageFilter {
	void execute(Message message);
}
