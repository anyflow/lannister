package net.anyflow.lannister.serialization;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;

import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Notification;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.TopicSubscription;

public class SerializableFactory implements PortableFactory {
	public static final int ID = 1;

	@Override
	public Portable create(int classId) {
		switch (classId) {
		case Message.ID:
			return new Message();

		case InboundMessageStatus.ID:
			return new InboundMessageStatus();

		case OutboundMessageStatus.ID:
			return new OutboundMessageStatus();

		case Session.ID:
			return new Session();

		case Notification.ID:
			return new Notification();

		case Topic.ID:
			return new Topic();

		case TopicSubscriber.ID:
			return new TopicSubscriber();

		case TopicSubscription.ID:
			return new TopicSubscription();

		default:
			return null;
		}
	}
}
