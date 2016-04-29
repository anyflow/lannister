package net.anyflow.lannister.message;

public enum SenderTargetStatus {
	TO_PUB((byte) 0),
	TO_REL((byte) 1),
	TO_BE_REMOVED((byte) 3);

	private byte id;

	private SenderTargetStatus(byte id) {
		this.id = id;
	}

	public byte id() {
		return id;
	}
}