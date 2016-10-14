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

package net.anyflow.lannister;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.serialization.SysValueSerializer;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscription;

public class Statistics {

	public static final Statistics INSTANCE = new Statistics();
	private static final int CENT = 100;

	private java.util.Map<String, SysValue> data; // key : topic name
	private Map<Criterion, Long> criterions;
	private final Runtime runtime;
	private final DecimalFormat decimalFormatter;
	private final NumberFormat defaultFormatter;
	private final com.sun.management.OperatingSystemMXBean osBean;

	@FunctionalInterface
	@JsonSerialize(using = SysValueSerializer.class)
	public interface SysValue {
		String value();
	}

	private class RawSysValue implements SysValue {
		private Criterion criterion;

		private RawSysValue(Criterion criterion) {
			this.criterion = criterion;
		}

		@Override
		public String value() {
			return NumberFormat.getNumberInstance().format(criterions.get(criterion));
		}
	}

	public enum Criterion {
		BROKER_START_TIME,
		BYTE_RECEIVED,
		BYTE_SENT,
		CLIENTS_MAXIMUM,
		MESSAGES_RECEIVED,
		MESSAGES_SENT,
		MESSAGES_PUBLISH_DROPPED,
		MESSAGES_PUBLISH_RECEIVED,
		MESSAGES_PUBLISH_SENT;
	}

	private Statistics() {
		this.data = Maps.newHashMap();
		this.criterions = ClusterDataFactory.INSTANCE.createMap("statistics");
		this.runtime = Runtime.getRuntime();
		this.osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		this.decimalFormatter = new DecimalFormat("#,###.0");
		this.defaultFormatter = NumberFormat.getNumberInstance();

		initializeCriterions();

		initializeTopics();
	}

	public java.util.Map<String, SysValue> data() {
		return data;
	}

	public void add(Criterion criterion, long size) {
		Lock lock = ClusterDataFactory.INSTANCE.createLock(criterion.toString());

		lock.lock();
		try {
			Long val = criterions.get(criterion);
			val += size;

			criterions.put(criterion, val);
		}
		finally {
			lock.unlock();
		}
	}

	public void setMaxActiveClients(long current) {
		Lock lock = ClusterDataFactory.INSTANCE.createLock(Criterion.CLIENTS_MAXIMUM.toString());

		lock.lock();
		try {
			Long prev = criterions.get(Criterion.CLIENTS_MAXIMUM);

			if (prev < current) {
				criterions.put(Criterion.CLIENTS_MAXIMUM, current);
			}
		}
		finally {
			lock.unlock();
		}
	}

	private void initializeCriterions() {
		if (criterions.get(Criterion.BROKER_START_TIME) == null) {
			criterions.put(Criterion.BROKER_START_TIME, new Date().getTime());
		}

		initialize(Criterion.BYTE_RECEIVED);
		initialize(Criterion.BYTE_SENT);
		initialize(Criterion.CLIENTS_MAXIMUM);
		initialize(Criterion.MESSAGES_RECEIVED);
		initialize(Criterion.MESSAGES_SENT);
		initialize(Criterion.MESSAGES_PUBLISH_DROPPED);
		initialize(Criterion.MESSAGES_PUBLISH_RECEIVED);
		initialize(Criterion.MESSAGES_PUBLISH_SENT);
	}

	private void initialize(Criterion criterion) {
		if (criterions.get(criterion) != null) { return; }

		criterions.put(criterion, 0l);
	}

	private void initializeTopics() {
		// MESSAGE
		data.put("$SYS/broker/messages/received", new RawSysValue(Criterion.MESSAGES_RECEIVED));
		data.put("$SYS/broker/messages/sent", new RawSysValue(Criterion.MESSAGES_SENT));
		data.put("$SYS/broker/messages/publish/dropped", new RawSysValue(Criterion.MESSAGES_PUBLISH_DROPPED));
		data.put("$SYS/broker/messages/publish/received", new RawSysValue(Criterion.MESSAGES_PUBLISH_RECEIVED));
		data.put("$SYS/broker/messages/publish/sent", new RawSysValue(Criterion.MESSAGES_PUBLISH_SENT));
		data.put("$SYS/broker/messages/retained/count", () -> defaultFormatter
				.format(Topic.NEXUS.map().values().stream().filter(t -> t.retainedMessage() != null).count()));

		// CLIENT
		data.put("$SYS/broker/clients/maximum", new RawSysValue(Criterion.CLIENTS_MAXIMUM));
		data.put("$SYS/broker/clients/connected", () -> {
			long current = Session.NEXUS.map().values().stream().filter(s -> s.isConnected(false)).count();

			setMaxActiveClients(current);

			return defaultFormatter.format(current);
		});
		data.put("$SYS/broker/clients/disconnected", () -> defaultFormatter
				.format(Session.NEXUS.map().values().stream().filter(s -> !s.isConnected(false)).count()));
		data.put("$SYS/broker/clients/total", () -> defaultFormatter.format(Session.NEXUS.map().size()));

		// STATIC
		data.put("$SYS/broker/version", () -> Settings.INSTANCE.version());
		data.put("$SYS/broker/timestamp", () -> Settings.INSTANCE.buildTime());
		data.put("$SYS/broker/changeset",
				() -> Settings.INSTANCE.commitId() + " | " + Settings.INSTANCE.commitIdDescribe());

		// ETC
		data.put("$SYS/broker/load/bytes/received", new RawSysValue(Criterion.BYTE_RECEIVED));
		data.put("$SYS/broker/load/bytes/sent", new RawSysValue(Criterion.BYTE_SENT));
		data.put("$SYS/broker/subscriptions/count", () -> defaultFormatter.format(TopicSubscription.NEXUS.size()));
		data.put("$SYS/broker/time", () -> new Date().toString());
		data.put("$SYS/broker/uptime", () -> defaultFormatter
				.format((double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME)) / (double) 1000));

		// RATE
		data.put("$SYS/broker/messages/received/inSecond", () -> {
			Long numerator = criterions.get(Criterion.MESSAGES_RECEIVED);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});
		data.put("$SYS/broker/messages/sent/inSecond", () -> {
			Long numerator = criterions.get(Criterion.MESSAGES_SENT);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});
		data.put("$SYS/broker/load/bytes/received/inSecond", () -> {
			Long numerator = criterions.get(Criterion.BYTE_RECEIVED);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});
		data.put("$SYS/broker/load/bytes/sent/inSecond", () -> {
			Long numerator = criterions.get(Criterion.BYTE_SENT);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});
		data.put("$SYS/broker/messages/publish/dropped/inSecond", () -> {
			Long numerator = criterions.get(Criterion.MESSAGES_PUBLISH_DROPPED);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});
		data.put("$SYS/broker/messages/publish/received/inSecond", () -> {
			Long numerator = criterions.get(Criterion.MESSAGES_PUBLISH_RECEIVED);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});
		data.put("$SYS/broker/messages/publish/sent/inSecond", () -> {
			Long numerator = criterions.get(Criterion.MESSAGES_PUBLISH_SENT);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});
		data.put("$SYS/broker/messages/publish/sent/inSecond", () -> {
			Long numerator = criterions.get(Criterion.MESSAGES_PUBLISH_SENT);
			Double denominator = (double) (new Date().getTime() - criterions.get(Criterion.BROKER_START_TIME))
					/ (double) 1000;

			return decimalFormatter.format((double) numerator / denominator);
		});

		// SYSTEM
		data.put("$SYS/broker/node/" + ClusterDataFactory.INSTANCE.currentId() + "/system/processor/count",
				() -> defaultFormatter.format(Runtime.getRuntime().availableProcessors()));
		data.put("$SYS/broker/node/" + ClusterDataFactory.INSTANCE.currentId() + "/load/cpu/system/percent",
				() -> decimalFormatter.format(osBean.getSystemCpuLoad() * CENT));
		data.put("$SYS/broker/node/" + ClusterDataFactory.INSTANCE.currentId() + "/load/cpu/jvm/percent",
				() -> decimalFormatter.format(osBean.getProcessCpuLoad() * CENT));
		data.put("$SYS/broker/node/" + ClusterDataFactory.INSTANCE.currentId() + "/load/thread/active/count",
				() -> defaultFormatter.format(java.lang.Thread.activeCount()));
		data.put("$SYS/broker/node/" + ClusterDataFactory.INSTANCE.currentId() + "/load/memory/max/byte",
				() -> defaultFormatter.format(runtime.maxMemory()));
		data.put("$SYS/broker/node/" + ClusterDataFactory.INSTANCE.currentId() + "/load/memory/total/byte",
				() -> defaultFormatter.format(runtime.totalMemory()));
		data.put("$SYS/broker/node/" + ClusterDataFactory.INSTANCE.currentId() + "/load/memory/free/byte",
				() -> defaultFormatter.format(runtime.freeMemory()));
	}
}