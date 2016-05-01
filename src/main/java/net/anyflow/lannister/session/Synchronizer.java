package net.anyflow.lannister.session;

public class Synchronizer {

	private final Session session;

	protected Synchronizer(Session session) {
		this.session = session;
	}

	protected void execute() {
		Session.NEXUS.persist(session);
	}
}