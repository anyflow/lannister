package net.anyflow.lannister.message;

public class SentMessageStatus extends MessageStatus {

	private static final long serialVersionUID = 164087382992696945L;

	private int originalMessageId;
	private SenderTargetStatus targetStatus;

	public SentMessageStatus(String clientId, int messageId, int originalMessageId) {
		super(clientId, messageId);

		this.originalMessageId = originalMessageId;
		this.targetStatus = SenderTargetStatus.TO_PUB;
	}

	public int originalMessageId() {
		return originalMessageId;
	}

	public SenderTargetStatus targetStatus() {
		return targetStatus;
	}

	public void targetStatus(SenderTargetStatus targetStatus) {
		this.targetStatus = targetStatus;
	}
}