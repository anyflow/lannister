package net.anyflow.lannister.cluster;

import java.util.stream.Stream;

import com.hazelcast.core.ISet;

public class HazelcastSet<V> implements Set<V> {
	private final ISet<V> engine;

	protected HazelcastSet(String name) {
		engine = Hazelcast.INSTANCE.getSet(name);
	}

	@Override
	public Stream<V> stream() {
		return engine.stream();
	}

	@Override
	public boolean remove(V value) {
		return engine.remove(value);
	}

	@Override
	public boolean add(V value) {
		return engine.add(value);
	}

	@Override
	public void dispose() {
		engine.destroy();
	}
}