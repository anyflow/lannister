package net.anyflow.lannister;

import org.junit.rules.ExternalResource;

public class Server extends ExternalResource {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Server.class);

	@Override
	protected void before() throws Throwable {
		Application.main(null);
	}

	@Override
	protected void after() {
		Application.instance().shutdown();
	}
}