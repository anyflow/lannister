package net.anyflow.lannister.sys;

import java.util.Date;
import java.util.Map;

import com.google.common.collect.Maps;

import net.anyflow.lannister.Settings;

public class Statistics {

	public static Statistics SELF;

	public static final String TOPICFILTER_BYTE_RECEIVED = "$SYS/broker/load/bytes/received";
	public static final String TOPICFILTER_BYTE_SENT = "$SYS/broker/load/bytes/sent";
	public static final String TOPICFILTER_CLIENTS_CONNECTED = "$SYS/broker/clients/connected";
	public static final String TOPICFILTER_CLIENTS_DISCONNECTED = "$SYS/broker/clients/disconnected";
	public static final String TOPICFILTER_CLIENTS_MAXIMUM = "$SYS/broker/clients/maximum";
	public static final String TOPICFILTER_CLIENTS_TOTAL = "$SYS/broker/clients/total";
	public static final String TOPICFILTER_MESSAGES_RECEIVED = "$SYS/broker/messages/received";
	public static final String TOPICFILTER_MESSAGES_SENT = "$SYS/broker/messages/sent";
	public static final String TOPICFILTER_PUBLISH_DROPPED = "$SYS/broker/messages/publish/dropped";
	public static final String TOPICFILTER_PUBLISH_RECEIVED = "$SYS/broker/messages/publish/received";
	public static final String TOPICFILTER_PUBLISH_SENT = "$SYS/broker/messages/publish/sent";
	public static final String TOPICFILTER_RETAINED_COUNT = "$SYS/broker/messages/retained/count";
	public static final String TOPICFILTER_SUBSCRIPTIONS_COUNT = "$SYS/broker/subscriptions/count";
	public static final String TOPICFILTER_BROKER_TIME = "$SYS/broker/time";
	public static final String TOPICFILTER_BROKER_UPTIME = "$SYS/broker/uptime";
	public static final String TOPICFILTER_BROKER_VERSION = "$SYS/broker/version";

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
		data.put(TOPICFILTER_BROKER_VERSION, new SysValue() {
			@Override
			public String value() {
				return Settings.SELF.getProperty("project.version");
			}
		});

		data.put(TOPICFILTER_BROKER_UPTIME, new SysValue() {
			@Override
			public String value() {
				return Double.toString((double) ((new Date()).getTime() - brokerStartTime.getTime()) / (double) 1000);
			}
		});

		data.put(TOPICFILTER_BROKER_TIME, new SysValue() {
			@Override
			public String value() {
				return (new Date()).toString();
			}
		});

		data.put(TOPICFILTER_CLIENTS_CONNECTED, new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});

		data.put(TOPICFILTER_CLIENTS_DISCONNECTED, new SysValue() {
			@Override
			public String value() {
				return "null";
			}
		});
	}

	public Map<String, SysValue> data() {
		return data;
	}
}