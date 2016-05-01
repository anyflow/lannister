package net.anyflow.lannister.serialization;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convertable to JSON format Object(String, byte array)
 * 
 */
public class Jsonizable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Jsonizable.class);
	protected static final String RUNTIME_EXCEPTION_MESSAGE = "Exception should not be occurred. See logs.";

	public static <T> T read(String json, Class<T> returnClass) throws JsonProcessingException, IOException {
		return read(json, returnClass, new ObjectMapper());
	}

	public static <T> T read(String json, TypeReference<T> typeReference) throws IOException {
		return read(json, typeReference, new ObjectMapper());
	}

	public static <T> T read(byte[] json, Class<T> returnClass) throws JsonProcessingException, IOException {
		return read(json, returnClass, new ObjectMapper());
	}

	public static <T> T read(String json, TypeReference<T> typeReference, ObjectMapper mapper) throws IOException {
		return mapper.reader().forType(typeReference).readValue(json);
	}

	public static <T> T read(String json, Class<T> returnClass, ObjectMapper mapper)
			throws JsonProcessingException, IOException {
		return mapper.reader().forType(returnClass).readValue(json);
	}

	public static <T> T read(byte[] json, Class<T> returnClass, ObjectMapper mapper)
			throws JsonProcessingException, IOException {
		return mapper.reader().forType(returnClass).readValue(json);
	}

	public String toJsonString() throws JsonProcessingException {
		return toJsonString(new ObjectMapper());
	}

	public byte[] toJsonByteArray() throws JsonProcessingException {
		return toJsonByteArray(new ObjectMapper());
	}

	public String toJsonString(ObjectMapper mapper) throws JsonProcessingException {
		return toJsonString(mapper, this);
	}

	public byte[] toJsonByteArray(ObjectMapper mapper) throws JsonProcessingException {
		return mapper.writer().writeValueAsBytes(this);
	}

	public static String toJsonString(Object target) throws JsonProcessingException {
		return toJsonString(new ObjectMapper(), target);
	}

	public String toJsonStringWithout(String... properties) throws JsonProcessingException {
		String json = toJsonString();
		if (properties == null) { return json; }

		try {
			JSONObject obj = new JSONObject(json);

			for (String prop : properties) {
				obj.remove(prop);
			}

			return obj.toString();
		}
		catch (JSONException e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}

	public static String toJsonString(ObjectMapper mapper, Object target) throws JsonProcessingException {
		return mapper.writer().writeValueAsString(target);
	}

	@Override
	public String toString() {
		try {
			return this.getClass().getSimpleName() + "|" + toJsonString();
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
}