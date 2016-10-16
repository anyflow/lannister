/*
 * Copyright 2016 The Lannister Project
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
package net.anyflow.lannister.cluster;

import java.io.IOException;
import java.util.HashSet;

import com.google.common.collect.Lists;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import net.anyflow.lannister.serialization.SerializableFactory;

public class SerializableIntegerSet extends HashSet<Integer> implements IdentifiedDataSerializable {
	private static final long serialVersionUID = -2853172691112908250L;
	public static final int ID = 10;

	public SerializableIntegerSet() {
	}

	public SerializableIntegerSet(Integer... items) {
		this();

		this.addAll(Lists.newArrayList(items));
	}

	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@Override
	public int getId() {
		return ID;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeInt(size());

		for (Integer item : this) {
			out.writeInt(item);
		}
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		int size = in.readInt();

		for (int i = 0; i < size; ++i) {
			this.add(in.readInt());
		}
	}
}