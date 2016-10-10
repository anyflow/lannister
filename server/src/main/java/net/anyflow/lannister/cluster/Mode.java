package net.anyflow.lannister.cluster;

public enum Mode {
	HAZELCAST("hazelcast"),
	IGNITE("ignite"),
	SINGLE("single");

	private String value;

	private Mode(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static Mode from(String value) {
		for (Mode item : values()) {
			if (item.value().equals(value)) { return item; }
		}

		return null;
	}
}
