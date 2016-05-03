package net.anyflow.lannister.topic;

import com.google.common.base.Strings;

public class TopicMatcher {
	public static boolean match(String topicFilter, String topicName) {
		if (Strings.isNullOrEmpty(topicFilter) || Strings.isNullOrEmpty(topicName)) { return false; }

		// TODO topic wildcard filtering

		return topicFilter.equals(topicName);
	}
}
