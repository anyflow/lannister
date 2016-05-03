package net.anyflow.lannister.topic;

import com.google.common.base.Strings;

public class TopicValidator {
	public static boolean isValidFilter(String topicFilter) {
		// TODO topic filter validation
		return Strings.isNullOrEmpty(topicFilter) == false;
	}

	public static boolean isValidName(String topicName) {
		// TODO topic filter validation
		return Strings.isNullOrEmpty(topicName) == false;
	}
}
