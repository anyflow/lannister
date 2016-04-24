package net.anyflow.lannister;

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

	public static <T> T read(String json, Class<T> returnClass) {
		return read(json, returnClass, new ObjectMapper());
	}

	public static <T> T read(String json, TypeReference<T> typeReference) {
		return read(json, typeReference, new ObjectMapper());
	}

	public static <T> T read(byte[] json, Class<T> returnClass) {
		return read(json, returnClass, new ObjectMapper());
	}

	public static <T> T read(String json, TypeReference<T> typeReference, ObjectMapper mapper) {

		try {
			return mapper.reader().withType(typeReference).readValue(json);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(RUNTIME_EXCEPTION_MESSAGE, e);
		}
	}

	public static <T> T read(String json, Class<T> returnClass, ObjectMapper mapper) {

		try {
			return mapper.reader().withType(returnClass).readValue(json);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(RUNTIME_EXCEPTION_MESSAGE, e);
		}
	}

	public static <T> T read(byte[] json, Class<T> returnClass, ObjectMapper mapper) {

		try {
			return mapper.reader().withType(returnClass).readValue(json);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(RUNTIME_EXCEPTION_MESSAGE, e);
		}
	}

	public String toJsonString() {
		return toJsonString(new ObjectMapper());
	}

	public byte[] toJsonByteArray() {
		return toJsonByteArray(new ObjectMapper());
	}

	public String toJsonString(ObjectMapper mapper) {
		return toJsonString(mapper, this);
	}

	public byte[] toJsonByteArray(ObjectMapper mapper) {
		try {
			return mapper.writer().writeValueAsBytes(this);
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(RUNTIME_EXCEPTION_MESSAGE, e);
		}
	}

	public static String toJsonString(Object target) {
		return toJsonString(new ObjectMapper(), target);
	}

	public String toJsonStringWithout(String... properties) {
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

	public static String toJsonString(ObjectMapper mapper, Object target) {
		try {
			return mapper.writer().writeValueAsString(target);
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(RUNTIME_EXCEPTION_MESSAGE, e);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "|" + toJsonString();
	}
}