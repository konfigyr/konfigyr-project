package com.konfigyr.assertions;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ThrowingConsumer;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link org.hamcrest.Matcher Hamcrest Matcher} that can be used with
 * {@link org.assertj.core.api.Assert Assert4J} API.
 *
 * @param <T> type of the value being asserted
 * @author Vladimir Spasic
 **/
public final class AssertMatcher<T> extends DiagnosingMatcher<T> {

	private final ThrowingConsumer<T> consumer;

	/**
	 * Creates a new {@link AssertMatcher} from the consumer function that would perform
	 * the assertions on the underlying object.
	 *
	 * @param <T> asserting object type
	 * @param consumer consumer function that performs the asserts, can't be {@code null}
	 * @return asserting matcher, never {@code null}
	 * @throws IllegalArgumentException when consumer is {@code null}
	 */
	public static <T> AssertMatcher<T> of(ThrowingConsumer<T> consumer) {
		Assert.notNull(consumer, "You need to specify a consumer to this asserting matcher");
		return new AssertMatcher<>(consumer);
	}

	/**
	 * Creates a new {@link AssertMatcher} from the consumer function that would perform the
	 * assertions on the underlying object using the {@link AbstractAssert Assert} that is
	 * created by the supplied {@link InstanceOfAssertFactory}.
	 *
	 * @param factory assert instance factory, can't be {@code null}
	 * @param consumer consumer function that performs the asserts, can't be {@code null}
	 * @param <T> asserting object type
	 * @param <A> assert type
	 * @return asserting matcher, never {@code null}
	 * @throws IllegalArgumentException when consumer or factory is {@code null}
	 */
	public static <T, A extends AbstractAssert<?, ?>> AssertMatcher<T> of(InstanceOfAssertFactory<T, A> factory, ThrowingConsumer<A> consumer) {
		Assert.notNull(factory, "You need to specify an assert factory");
		Assert.notNull(consumer, "You need to specify a consumer to this asserting matcher");

		return new AssertMatcher<>(o -> consumer.acceptThrows(factory.createAssert(o)));
	}

	private AssertMatcher(@NonNull ThrowingConsumer<T> consumer) {
		this.consumer = consumer;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected boolean matches(Object o, Description description) {
		try {
			consumer.accept((T) o);
		} catch (AssertionError e) {
			description.appendText("Failed to assert object: " + o + ".\n")
					.appendText(ExceptionUtils.getStackTrace(e));

			return false;
		}

		return true;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("Assert4J assertion to not throw an assertion error");
	}
}
