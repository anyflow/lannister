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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class HazelcastStreamSerializer<T, S> extends HazelcastSerializer<T, S> implements StreamSerializer<T> {

	protected HazelcastStreamSerializer(final Class<T> type, int typeId) {
		super(type, typeId, null);
	}

	protected HazelcastStreamSerializer(final Class<T> type, int typeId, Supplier<? extends S> supplier) {
		super(type, typeId, supplier);
	}

	@Override
	public void write(ObjectDataOutput objectDataOutput, T object) throws IOException {
		streamedWrite((OutputStream) objectDataOutput, object);
	}

	@Override
	public T read(ObjectDataInput objectDataInput) throws IOException {
		return streamedRead((InputStream) objectDataInput);
	}

	protected void streamedWrite(OutputStream outputStream, T object) throws IOException {
	}

	protected T streamedRead(InputStream inputStream) throws IOException {
		return null;
	}
}