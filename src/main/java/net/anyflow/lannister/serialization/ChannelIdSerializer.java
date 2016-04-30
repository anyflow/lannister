package net.anyflow.lannister.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.netty.channel.ChannelId;

public final class ChannelIdSerializer extends com.fasterxml.jackson.databind.JsonSerializer<ChannelId> {

	@Override
	public void serialize(ChannelId value, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonProcessingException {

		generator.writeString(value.toString());
	}
}