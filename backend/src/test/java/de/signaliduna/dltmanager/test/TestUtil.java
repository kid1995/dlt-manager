package de.signaliduna.dltmanager.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.AbstractStringAssert;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtil {

	private static final ObjectMapper PRETTY_PRINT_MAPPER = new ObjectMapper();
	private static final ObjectMapper NORMAL_MAPPER = new ObjectMapper();

	static {
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
		PRETTY_PRINT_MAPPER.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		PRETTY_PRINT_MAPPER.configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);
		PRETTY_PRINT_MAPPER.configure(JsonNodeFeature.WRITE_PROPERTIES_SORTED, false);
		PRETTY_PRINT_MAPPER.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
		PRETTY_PRINT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		PRETTY_PRINT_MAPPER.setDefaultPrettyPrinter(prettyPrinter);

		NORMAL_MAPPER.registerModule(new JavaTimeModule());
	}

	public static <T> ArgumentCaptor<T> captureSingleArg(Class<T> clazz, Consumer<ArgumentCaptor<T>> consumer) {
		ArgumentCaptor<T> captor = ArgumentCaptor.forClass(clazz);
		consumer.accept(captor);
		return captor;
	}

	/**
	 * Provides an {@code isEqualTo()} assertion based on a normalized JSON representation of the given value.
	 */
	public static <T> NormalizedJsonStringAssert assertThatJsonStringOf(T actual) {
		return new NormalizedJsonStringAssert(asPrettyJsonString(actual), NormalizedJsonStringAssert.class);
	}

	/**
	 * Provides an {@code isEqualTo()} assertion based on a normalized JSON representation of the given JSON string.
	 */
	public static NormalizedJsonStringAssert assertThatJsonString(String actualJsonString) {
		return new NormalizedJsonStringAssert(prettyfyJsonString(actualJsonString), NormalizedJsonStringAssert.class);
	}

	public static <T> String asJsonString(T data) {
		try {
			return NORMAL_MAPPER.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> String asPrettyJsonString(T data) {
		try {
			return PRETTY_PRINT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String prettyfyJsonString(String inputJsonString) {
		final JsonNode json;
		try {
			json = PRETTY_PRINT_MAPPER.reader().readTree(inputJsonString);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return asPrettyJsonString(json);
	}

	public static class NormalizedJsonStringAssert extends AbstractStringAssert<NormalizedJsonStringAssert> {
		protected NormalizedJsonStringAssert(String actual, Class<?> selfType) {
			super(actual, selfType);
		}

		@Override
		public NormalizedJsonStringAssert isEqualTo(String expected) {
			return super.isEqualTo(prettyfyJsonString(expected));
		}
	}

	private TestUtil() {
	}
}
