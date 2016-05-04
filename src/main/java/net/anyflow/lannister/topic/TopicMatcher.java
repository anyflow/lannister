package net.anyflow.lannister.topic;

import java.io.UnsupportedEncodingException;

import com.google.common.base.Strings;

public class TopicMatcher {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicMatcher.class);

	private static final String SEPARATOR = "/";
	private static final String MULTI_LEVEL_WILDCARD = "#";
	private static final String SINGLE_LEVEL_WILDCARD = "+";

	private static final int MIN_LENGTH = 1;
	private static final int MAX_LENGTH = 65535;
	private static final char NULL = '\u0000';

	private static final int INDEX_NOT_FOUND = -1;

	public static final String MULTI_LEVEL_WILDCARD_PATTERN = SEPARATOR + MULTI_LEVEL_WILDCARD;
	public static final String TOPIC_WILDCARDS = MULTI_LEVEL_WILDCARD + SINGLE_LEVEL_WILDCARD;

	public static boolean isValid(String token, boolean allowWildcard) {
		int length = 0;
		try {
			length = token.getBytes("UTF-8").length;
		}
		catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
			return false;
		}

		if (length < MIN_LENGTH || length > MAX_LENGTH) {
			logger.error("Invalid topic length [length={}", length);
			return false;
		}

		if (allowWildcard) {
			if (token.equals(MULTI_LEVEL_WILDCARD) || token.equals(SINGLE_LEVEL_WILDCARD)) { return true; }

			if (countMatches(token, MULTI_LEVEL_WILDCARD) > 1
					|| (token.contains(MULTI_LEVEL_WILDCARD) && !token.endsWith(MULTI_LEVEL_WILDCARD_PATTERN))) {
				logger.error("Invalid usage of multi-level wildcard in topic string: {}", token);
				return false;
			}
		}

		return isValidSingleLevelWildcard(token);
	}

	public static int countMatches(String str, String sub) {
		if (Strings.isNullOrEmpty(str) || Strings.isNullOrEmpty(sub)) { return 0; }

		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf(sub, idx)) != INDEX_NOT_FOUND) {
			count++;
			idx += sub.length();
		}

		return count;
	}

	private static boolean isValidSingleLevelWildcard(String topicString) {
		char singleLevelWildcardChar = SINGLE_LEVEL_WILDCARD.charAt(0);
		char topicLevelSeparatorChar = SEPARATOR.charAt(0);

		char[] chars = topicString.toCharArray();
		int length = chars.length;
		char prev = NULL, next = NULL;

		for (int i = 0; i < length; i++) {
			prev = (i - 1 >= 0) ? chars[i - 1] : NULL;
			next = (i + 1 < length) ? chars[i + 1] : NULL;

			if ((chars[i] == singleLevelWildcardChar) && (prev != topicLevelSeparatorChar && prev != NULL
					|| next != topicLevelSeparatorChar && next != NULL)) {
				logger.error("Invalid usage of single-level wildcard in topic string '{}'!", topicString);
				return false;
			}
		}

		return true;
	}

	public static boolean match(String topicFilter, String topicName) {
		if (!TopicMatcher.isValid(topicFilter, true) || !TopicMatcher.isValid(topicName, false)) { return false; }

		String[] fts = topicFilter.split("/");
		String[] nts = topicName.split("/");

		for (int i = 0; i < nts.length; ++i) {
			if (i >= fts.length) {
				return false;
			}
			else if (fts[i].equals(MULTI_LEVEL_WILDCARD)) {
				return true;
			}
			else if (fts[i].equals(SINGLE_LEVEL_WILDCARD)) {
				continue;
			}
			else if (fts[i].equals(nts[i])) {
				continue;
			}
			else {
				return false;
			}
		}

		return true;
	}
}