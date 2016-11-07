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

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mapdb.DBMaker;

import com.hazelcast.nio.serialization.Data;

/**
 * Factory for off-heap maps
 */
public class HazelcastOffheap {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HazelcastOffheap.class);

	static AtomicBoolean logged = new AtomicBoolean(false);

	/**
	 * Creates new off-heap map for HZ, called from instrumented code
	 */
	public static ConcurrentMap<Data, Record<?>> defaultRecordStoreRecords() {

		if (!logged.getAndSet(true)) {
			logger.info("mapdb-hz-offheap: MapDB HashMap instantiated. It works!");
		}

		return DBMaker.memoryDirectDB().make().hashMap("storage").keySerializer(new MapDBDataSerializer())
				.valueSerializer(new MapDBDataRecordSerializer()).counterEnable().createOrOpen();
	}
}
