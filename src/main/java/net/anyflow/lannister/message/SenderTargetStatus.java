package net.anyflow.lannister.message;

public enum SenderTargetStatus {
	TO_PUB((byte) 0),
	TO_REL((byte) 1),
	TO_BE_REMOVED((byte) 3);

	private byte value;

	private SenderTargetStatus(byte value) {
		this.value = value;
	}

	public byte value() {
		return value;
	}

	public static SenderTargetStatus valueOf(byte value) {
		for (SenderTargetStatus q : values()) {
			if (q.value == value) { return q; }
		}
		throw new IllegalArgumentException("invalid SenderTargetStatus: " + value);
	}
}