package net.anyflow.lannister.message;

public class ReceivedMessageStatus extends MessageStatus {

	private static final long serialVersionUID = 1314442477307044434L;

	private ReceiverTargetStatus targetStatus;

	public ReceivedMessageStatus(String clientId, int messageId) {
		super(clientId, messageId);

		targetStatus = ReceiverTargetStatus.TO_ACK;
	}

	public ReceiverTargetStatus targetStatus() {
		return targetStatus;
	}

	public void targetStatus(ReceiverTargetStatus targetStatus) {
		this.targetStatus = targetStatus;
	}
}
