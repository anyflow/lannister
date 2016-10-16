package net.anyflow.lannister.cluster;

import java.util.stream.Stream;

public interface Set<V> {
	Stream<V> stream();

	boolean remove(V value);

	boolean add(V value);

	void dispose();
}