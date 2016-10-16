package net.anyflow.lannister.cluster;

import java.util.stream.Stream;

import com.google.common.collect.Sets;

public class NativeSet<V> implements Set<V> {
	private final java.util.Set<V> engine;

	protected NativeSet() {
		engine = Sets.newHashSet();
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
		// DO NOTHING
	}
}