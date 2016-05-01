package net.anyflow.lannister.packetreceiver;

import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.Topics.ClientType;

public class PubCompReceiver {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubCompReceiver.class);

	public static PubCompReceiver SHARED = new PubCompReceiver();

	private PubCompReceiver() {
	}

	protected void handle(Session session, int messageId) {
		Topic topic = Topic.NEXUS.get(session.clientId(), messageId, ClientType.SUBSCRIBER);
		if (topic == null) {
			logger.error("Topic does not exist : [clientId={}, messageId={}]", session.clientId(), messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		final TopicSubscriber topicSubscriber = topic.subscribers().get(session.clientId());

		OutboundMessageStatus status = topicSubscriber.sentOutboundMessageStatuses().get(messageId);

		if (status == null) {
			logger.error("No message status to REMOVE(QoS2) : [clientId={}, messageId={}]", session.clientId(),
					messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}
		if (status.targetStatus() != OutboundMessageStatus.Status.TO_BE_REMOVED) {
			logger.error("Invalid status to REMOVE(QoS2) : [clientId={}, messageId={}, status={}]", session.clientId(),
					messageId, status);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		topicSubscriber.removeOutboundMessageStatus(messageId);
		logger.debug("Outbound message status REMOVED : [clientId={}, messageId={}]", session.clientId(), messageId);
	}
}