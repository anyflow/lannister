package net.anyflow.lannister.message;

public enum ReceiverTargetStatus {
	TO_ACK((byte) 0), // RECEIVER acknowlegement
	TO_REC((byte) 1), // RECEIVER received
	TO_COMP((byte) 2); // RECEIVER complete

	private byte value;

	private ReceiverTargetStatus(byte value) {
		this.value = value;
	}

	public byte value() {
		return value;
	}

	public static ReceiverTargetStatus valueOf(byte value) {
		for (ReceiverTargetStatus q : values()) {
			if (q.value == value) { return q; }
		}
		throw new IllegalArgumentException("invalid ReceiverTargetStatus: " + value);
	}
}
