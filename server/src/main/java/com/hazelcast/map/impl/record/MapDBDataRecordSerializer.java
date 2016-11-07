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
import org.mapdb.Serializer;

public class MapDBDataRecordSerializer implements Serializer<Record<?>>, Serializable {

	private static final long serialVersionUID = -3197831551063625770L;

	final static MapDBDataSerializer ds = new MapDBDataSerializer();

	@Override
	public int compare(Record<?> o1, Record<?> o2) {
		return o1.hashCode() - o2.hashCode();
	}

	@Override
	public void serialize(DataOutput2 out, Record<?> value) throws IOException {
		DataRecordWithStats w = (DataRecordWithStats) value;
		ds.serialize(out, w.getKey());
		ds.serialize(out, w.getValue());
	}

	@Override
	public Record<?> deserialize(DataInput2 input, int available) throws IOException {
		return new DataRecordWithStats(ds.deserialize(input, -1));
	}
}
