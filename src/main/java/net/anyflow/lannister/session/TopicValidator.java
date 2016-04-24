package net.anyflow.lannister.session;

import com.google.common.base.Strings;

public class TopicValidator {
	public static boolean isATopicName(String topicName) {
		return Strings.isNullOrEmpty(topicName) == false;
	}
}
