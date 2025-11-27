package com.konfigyr.feature;

import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FeatureValueConverter {

	private static final Map<Pattern, Function<Matcher, FeatureValue>> PATTERN_FACTORIES = Map.of(
			Pattern.compile("^unlimited+$"), FeatureValueConverter::unlimited,
			Pattern.compile("(?<value>^\\d+$)"), FeatureValueConverter::limited,
			Pattern.compile("^(?<value>\\d+)/(?<unit>[smhd])$"), FeatureValueConverter::rate
	);

	private FeatureValueConverter() {
		// noop
	}

	@SuppressWarnings("unchecked")
	static <T extends FeatureValue> T from(String value) {
		Assert.hasText(value, "Feature value must not be null or empty");

		final Iterator<Pattern> patterns = PATTERN_FACTORIES.keySet().iterator();
		FeatureValue result = null;

		while (result == null && patterns.hasNext()) {
			final Pattern pattern = patterns.next();
			final Matcher matcher = pattern.matcher(value);

			if (matcher.matches()) {
				result = PATTERN_FACTORIES.get(pattern).apply(matcher);
			}
		}

		if (result == null) {
			throw new IllegalArgumentException("Value '" + value + "' is not a valid feature value");
		}

		return (T) result;
	}

	@NonNull
	static String to(FeatureValue value) {
		Assert.notNull(value, "Feature value must not be null");

		return switch (value) {
			case LimitedFeatureValue it -> it.isLimited() ? String.valueOf(it.get()) : "unlimited";
			case RateLimitFeatureValue it -> it.rate() + "/" + it.unit().symbol();
		};
	}

	@NonNull
	private static LimitedFeatureValue unlimited(@NonNull Matcher matcher) {
		return FeatureValue.unlimited();
	}

	@NonNull
	private static LimitedFeatureValue limited(@NonNull Matcher matcher) {
		final String value = matcher.group("value");
		return FeatureValue.limited(Long.parseLong(value));
	}

	@NonNull
	private static RateLimitFeatureValue rate(@NonNull Matcher matcher) {
		final String value = matcher.group("value");
		final String unit = matcher.group("unit");

		return FeatureValue.rateLimit(Long.parseLong(value), DurationUnit.from(unit));
	}

	static final class Serializer extends StdSerializer<FeatureValue> {

		Serializer() {
			super(FeatureValue.class);
		}

		@Override
		public void serialize(FeatureValue value, JsonGenerator generator, SerializationContext provider) {
			generator.writeString(to(value));
		}
	}

	static final class Deserializer extends StdDeserializer<FeatureValue> {

		Deserializer() {
			super(FeatureValue.class);
		}

		@Override
		public FeatureValue deserialize(JsonParser p, DeserializationContext context) throws JacksonException {
			return from(p.getString());
		}
	}

}
