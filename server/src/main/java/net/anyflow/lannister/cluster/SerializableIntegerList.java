package net.anyflow.lannister.cluster;

import java.io.IOException;
import java.util.ArrayList;

import com.google.common.collect.Lists;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import net.anyflow.lannister.serialization.SerializableFactory;

public class SerializableIntegerList extends ArrayList<Integer> implements IdentifiedDataSerializable {
	private static final long serialVersionUID = -2853172691112908250L;
	public static final int ID = 10;

	public SerializableIntegerList() {
	}

	public SerializableIntegerList(Integer... items) {
		this();

		this.addAll(Lists.newArrayList(items));
	}

	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@Override
	public int getId() {
		return ID;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeInt(size());

		for (Integer item : this) {
			out.writeInt(item);
		}
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		int size = in.readInt();

		for (int i = 0; i < size; ++i) {
			this.add(in.readInt());
		}
	}
}