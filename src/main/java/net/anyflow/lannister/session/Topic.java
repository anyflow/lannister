package net.anyflow.lannister.session;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import net.anyflow.lannister.Jsonizable;

public class Topic extends Jsonizable implements java.io.Serializable {

	private static final long serialVersionUID = -3335949846595801533L;

	private static Topics TOPICS = new Topics();

	@JsonProperty
	private String name;
	@JsonProperty
	private Message retainedMessage;
	@JsonProperty
	private Set<String> subscribers;

	public Topic(String name) {
		this.name = name;
		this.retainedMessage = null;

		this.subscribers = Collections.synchronizedSet(Sets.newHashSet());
	}

	public String name() {
		return name;
	}

	public Message retainedMessage() {
		return retainedMessage;
	}

	public void setRetainedMessage(Message message) {
		this.retainedMessage = message;
	}

	public ImmutableSet<String> subscribers() {
		return ImmutableSet.copyOf(subscribers);
	}

	public void addSubscriber(String clientId, boolean persist) {
		if (subscribers.add(clientId) == false) { return; }

		if (persist) {
			TOPICS.put(this);
		}
	}

	public void removeSubscriber(String clientId, boolean persist) {
		if (subscribers.remove(clientId) == false) { return; }

		// TODO should be this topic remained in spite of no subscriber?

		if (persist) {
			TOPICS.put(this);
		}
	}

	public void publish(Message message) {
		if (message.isRetain()) {
			// [MQTT-3.3.1-10],[MQTT-3.3.1-11]
			retainedMessage = message.message().length > 0
					? new Message(null, message.topicName(), message.message(), null, true) : null;

			TOPICS.put(this);
		}
		else {
			// do nothing [MQTT-3.3.1-12]
		}

		subscribers.stream().parallel().forEach(clientId -> {
			Session session = Session.getByClientId(clientId, true);
			session.published(message);
		});
	}

	public static Topic get(String topicName) {
		return TOPICS.get(topicName);
	}

	public static boolean isValid(String topicName) {
		// TODO topic name validation
		return Strings.isNullOrEmpty(topicName) == false;
	}

	public static Topic put(Topic topic) {
		Session.topicAdded(topic);

		// TODO should be added in case of no subscriber & no retained Message?
		return TOPICS.put(topic);
	}

	public static ImmutableList<Topic> matches(String topicFilter) {
		return TOPICS.matches(topicFilter);
	}
}