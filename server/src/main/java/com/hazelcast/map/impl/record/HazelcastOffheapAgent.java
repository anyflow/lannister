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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import javassist.ClassPool;
import javassist.CtClass;
import net.anyflow.lannister.Application;

/**
 * Java agent which modifies classes to use MapDB off-heap maps
 *
 * <a href="http://www.javassist.org/">javassist</a>.
 * 
 * <p>
 * inspired by
 * http://today.java.net/article/2008/04/22/add-logging-class-load-time-java-
 * instrumentation
 * </p>
 * <p>
 * also inspired by
 * https://github.com/kreyssel/maven-examples/tree/master/javaagent
 * </p>
 */
public class HazelcastOffheapAgent implements ClassFileTransformer {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HazelcastOffheapAgent.class);

	static final String DEFAULT_RECORD_STORE = "com/hazelcast/map/impl/recordstore/AbstractRecordStore";

	/**
	 * add agent
	 */
	public static void premain(final String agentArgument, final Instrumentation instrumentation) {
		Thread.currentThread().setName("agent");

		org.apache.log4j.xml.DOMConfigurator
				.configure(Application.class.getClassLoader().getResource("lannister.log4j.xml"));

		logger.info("Hazelcast Offheap agent started.");

		instrumentation.addTransformer(new HazelcastOffheapAgent());
	}

	/**
	 * instrument class
	 */
	@Override
	public byte[] transform(final ClassLoader loader, final String className, final Class<?> clazz,
			final java.security.ProtectionDomain domain, final byte[] bytes) {

		if (DEFAULT_RECORD_STORE.equals(className)) { return doDefaultRecordStore(className, clazz, bytes); }

		return bytes;
	}

	private byte[] doDefaultRecordStore(final String name, final Class<?> clazz, byte[] b) {
		logger.info("mapdb-hz-offheap agent: starting code injection");

		ClassPool pool = ClassPool.getDefault();
		CtClass cl = null;

		try {
			cl = pool.makeClass(new java.io.ByteArrayInputStream(b));

			final String CONSTRUCTOR = "storage=" + HazelcastOffheap.class.getName() + ".defaultRecordStoreRecords();";

			cl.getDeclaredConstructors()[0].insertAfter(CONSTRUCTOR);

			b = cl.toBytecode();

			logger.info("mapdb-hz-offheap agent: code injected.");

		}
		catch (Exception e) {
			logger.error("Could not instrument Hazelcast to use off-heap store " + name, e);
			throw new RuntimeException("Could not instrument Hazelcast to use off-heap store ", e);
		}
		finally {
			if (cl != null) {
				cl.detach();
			}
		}

		return b;
	}
}
