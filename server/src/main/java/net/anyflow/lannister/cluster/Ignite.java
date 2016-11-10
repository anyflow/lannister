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

import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;

public class Ignite {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Hazelcast.class);

	@SuppressWarnings("unused")
	private static final String CONFIG_NAME = "lannister.ignite.xml";

	public static final Ignite INSTANCE = new Ignite();

	private org.apache.ignite.Ignite substance;

	private Ignite() {
		substance = Ignition.start();
	}

	public <K, V> IgniteCache<K, V> getCache(String name) {
		return substance.getOrCreateCache(name);
	}
}
