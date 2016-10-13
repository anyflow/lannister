package net.anyflow.lannister.cluster;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;

public class Ignite {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Hazelcast.class);

	@SuppressWarnings("unused")
	private static final String CONFIG_NAME = "lannister.ignite.xml";

	public static final Ignite INSTANCE = new Ignite();

	private org.apache.ignite.Ignite substance;

	private Ignite() {
		substance = Ignition.start();
	}

	public <K, V> IgniteCache<K, V> getCache(String name) {
		return substance.getOrCreateCache(name);
	}
}
