package net.anyflow.lannister.cluster;

import com.hazelcast.core.IdGenerator;

public class SingleIdGenerator implements IdGenerator {

	private final String name;
	private long id;

	public SingleIdGenerator(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void destroy() {
		// DO NOTHING
	}

	@Override
	public boolean init(long id) {
		if (id < 0) { return false; }

		this.id = id;
		return true;
	}

	@Override
	public long newId() {
		return ++id;
	}

	@Override
	public String getPartitionKey() {
		throw new Error("The method should not be called");
	}

	@Override
	public String getServiceName() {
		throw new Error("The method should not be called");
	}
}