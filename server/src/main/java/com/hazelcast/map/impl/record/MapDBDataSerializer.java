/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl.record;

import java.io.IOException;
import java.io.Serializable;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

import com.hazelcast.nio.serialization.Data;

public class MapDBDataSerializer implements org.mapdb.Serializer<Data>, Serializable {
	private static final long serialVersionUID = 104316465250330096L;

	@Override
	public int fixedSize() {
		return -1;
	}

	@Override
	public int compare(Data o1, Data o2) {
		return o1.hashCode() - o2.hashCode();
	}

	@Override
	public void serialize(DataOutput2 out, Data value) throws IOException {
		byte[] b = value.toByteArray();
		out.writeInt(b.length);
		out.write(b);
	}

	@Override
	public Data deserialize(DataInput2 input, int available) throws IOException {
		int size = input.readInt();
		byte[] b = new byte[size];

		input.readFully(b);

		return new DefaultData(b);
	}
}