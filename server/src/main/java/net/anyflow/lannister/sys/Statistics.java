package net.anyflow.lannister.sys;

import java.util.Date;
import java.util.Map;

import com.google.common.collect.Maps;

import net.anyflow.lannister.Settings;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class Statistics {

	public static Statistics SELF;

	static {
		SELF = new Statistics();
	}

	private Map<String, SysValue> data;
	private Date brokerStartTime;

	private Statistics() {
		this.data = Maps.newHashMap();
		this.brokerStartTime = new Date();

		initializeData();
	}

	private void initializeData() {
		data.put("$SYS/broker/load/bytes/received", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/load/bytes/sent", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/clients/connected", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/clients/disconnected", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/clients/maximum", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/clients/total", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/messages/received", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/messages/sent", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/messages/publish/dropped", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/messages/publish/received", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/messages/publish/sent", new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put("$SYS/broker/messages/retained/count", new SysValue() {
			@Override
			public String value() {
				return Long
						.toString(Topic.NEXUS.map().values().stream().filter(t -> t.retainedMessage() != null).count());
			}
		});

		data.put("$SYS/broker/subscriptions/count", new SysValue() {
			@Override
			public String value() {
				return Long.toString(Session.NEXUS.map().values().stream().map(s -> s.topicSubscriptions())
						.flatMap(s -> s.values().stream()).count());
			}
		});

		data.put("$SYS/broker/time", new SysValue() {
			@Override
			public String value() {
				return (new Date()).toString();
			}
		});

		data.put("$SYS/broker/uptime", new SysValue() {
			@Override
			public String value() {
				return Double.toString((double) ((new Date()).getTime() - brokerStartTime.getTime()) / (double) 1000);
			}
		});

		data.put("$SYS/broker/version", new SysValue() {
			@Override
			public String value() {
				return Settings.SELF.getProperty("project.version");
			}
		});
	}

	public Map<String, SysValue> data() {
		return data;
	}
}