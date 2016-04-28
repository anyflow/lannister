package net.anyflow.lannister.session;

public class Synchronizer {

	final Session session;

	protected Synchronizer(Session session) {
		this.session = session;
	}

	protected void execute() {
		if (session.isCleanSession()) { return; }

		session.persist();
	}
}