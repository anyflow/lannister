/*
 * Copyright 2014 Luca Burgazzoli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anyflow.lannister.serialization;

import java.util.function.Supplier;

import com.hazelcast.nio.serialization.Serializer;

public class HazelcastSerializer<T, S> implements Serializer, Supplier<S> {
	protected final Class<T> type;
	protected final int typeId;
	protected final ThreadLocal<S> threadLocal;

	protected HazelcastSerializer(final Class<T> type, int typeId) {
		this(type, typeId, null);
	}

	protected HazelcastSerializer(final Class<T> type, int typeId, Supplier<? extends S> supplier) {
		this.type = type;
		this.typeId = typeId;
		this.threadLocal = supplier != null ? ThreadLocal.withInitial(supplier) : null;
	}

	@Override
	public int getTypeId() {
		return typeId;
	}

	@Override
	public void destroy() {
	}

	public Class<T> getType() {
		return this.type;
	}

	@Override
	public S get() {
		return threadLocal.get();
	}
}