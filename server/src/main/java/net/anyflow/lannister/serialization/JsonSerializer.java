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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Serializer;
import com.hazelcast.nio.serialization.StreamSerializer;

public final class JsonSerializer<T> extends HazelcastSerializer<T, ObjectMapper> implements StreamSerializer<T> {
	public static final int TYPEID_JSON_PLAIN = 100;
	public static final int TYPEID_JSON_BINARY = 101;

	private JsonSerializer(Class<T> type, int typeId) {
		this(type, typeId, null);
	}

	private JsonSerializer(Class<T> type, int typeId, final JsonFactory factory) {
		super(type, typeId, () -> new ObjectMapper(factory));
	}

	@Override
	public void write(ObjectDataOutput out, T object) throws IOException {
		out.writeByteArray(get().writeValueAsBytes(object));
	}

	@Override
	public T read(ObjectDataInput in) throws IOException {
		return get().readValue(in.readByteArray(), getType());
	}

	public static <V> Serializer makeBinary(final Class<V> type) {
		return new JsonSerializer<>(type, TYPEID_JSON_BINARY, new SmileFactory());
	}

	public static <V> Serializer makePlain(final Class<V> type) {
		return new JsonSerializer<>(type, TYPEID_JSON_PLAIN);
	}
}