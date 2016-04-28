package net.anyflow.lannister.message;

public enum ReceiverTargetStatus {
	TO_ACK((byte) 0), // RECEIVER acknowlegement
	TO_REC((byte) 1), // RECEIVER received
	TO_COMP((byte) 2);// RECEIVER complete

	private byte id;

	private ReceiverTargetStatus(byte id) {
		this.id = id;
	}

	public byte id() {
		return id;
	}
}
