package de.signaliduna.dltmanager.test;

import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.JsonNodeFeature;
import org.assertj.core.api.AbstractStringAssert;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.function.Consumer;

public class TestUtil {

	private static final JsonMapper PRETTY_PRINT_MAPPER;
	private static final JsonMapper NORMAL_MAPPER;

	static {
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
		PRETTY_PRINT_MAPPER = JsonMapper.builder().configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
		.configure(JsonNodeFeature.WRITE_PROPERTIES_SORTED, false)
		.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
		.defaultPrettyPrinter(prettyPrinter).build();
		NORMAL_MAPPER = JsonMapper.builder().build();
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
		} catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> String asPrettyJsonString(T data) {
		try {
			return PRETTY_PRINT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
		} catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	public static String prettyfyJsonString(String inputJsonString) {
		final JsonNode json;
		try {
			json = PRETTY_PRINT_MAPPER.reader().readTree(inputJsonString);
		} catch (JacksonException e) {
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
