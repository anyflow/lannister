package net.anyflow.lannister.session;

public class Synchronizer {

	final Session session;

	public Synchronizer(Session session) {
		this.session = session;
	}

	public void execute() {
		if (session.isCleanSession()) { return; }

		session.persist();
	}
}