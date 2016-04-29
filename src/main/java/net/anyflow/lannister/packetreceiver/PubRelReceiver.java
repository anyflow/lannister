package net.anyflow.lannister.packetreceiver;

import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.message.MessageStatus;
import net.anyflow.lannister.message.ReceivedMessageStatus;
import net.anyflow.lannister.message.ReceiverTargetStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class PubRelReceiver {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubRelReceiver.class);

	public static PubRelReceiver SHARED = new PubRelReceiver();

	private PubRelReceiver() {
	}

	protected void handle(Session session, int messageId) {
		Topic topic = Topic.NEXUS.get(session.clientId(), messageId);
		if (topic == null) {
			logger.error("Topic does not exist : [clientId={}, messageId={}]", session.clientId(), messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		ReceivedMessageStatus status = topic.receivedMessageStatuses()
				.get(MessageStatus.key(session.clientId(), messageId));

		if (status == null) {
			logger.error("No message status to PUBCOMP : [clientId={}, messageId={}]", session.clientId(), messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}
		if (status.targetStatus() != ReceiverTargetStatus.TO_COMP) {
			logger.error("Invalid status to PUBCOMP : [clientId={}, messageId={}, status={}]", session.clientId(),
					messageId, status);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		session.send(MessageFactory.pubcomp(messageId)).addListener(f -> {
			topic.removeReceivedMessageStatus(session.clientId(), messageId);
			logger.debug("message REMOVED : [clientId={}, messageId={}]", session.clientId(), messageId);
		});
	}
}