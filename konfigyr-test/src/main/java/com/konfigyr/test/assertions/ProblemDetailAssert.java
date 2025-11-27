package com.konfigyr.test.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ThrowingConsumer;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Objects;

/**
 * Assert class that should be used to test {@link ProblemDetail}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ProblemDetailAssert extends AbstractObjectAssert<ProblemDetailAssert, ProblemDetail> {

	/**
	 * Creates a new {@link ProblemDetailAssert} with the given problem detail to check.
	 *
	 * @param problemDetail the actual value to verify
	 * @return Problem detail assert
	 */
	@NonNull
	public static ProblemDetailAssert assertThat(ProblemDetail problemDetail) {
		return new ProblemDetailAssert(problemDetail);
	}

	/**
	 * Create an {@link InstanceOfAssertFactory} that can be used to create {@link ProblemDetailAssert} for
	 * an asserted object.
	 *
	 * @return Problem detail assert factory
	 */
	@NonNull
	public static InstanceOfAssertFactory<ProblemDetail, ProblemDetailAssert> factory() {
		return new InstanceOfAssertFactory<>(ProblemDetail.class, ProblemDetailAssert::new);
	}

	ProblemDetailAssert(ProblemDetail problemDetail) {
		super(problemDetail, ProblemDetailAssert.class);
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching {@link HttpStatusCode}.
	 *
	 * @param status status code
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasStatus(HttpStatusCode status) {
		return hasStatus(status.value());
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching HTTP status code.
	 *
	 * @param status status code
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasStatus(int status) {
		isNotNull();

		if (status != actual.getStatus()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Problem Details should have a status of \"%s\" but was \"%s\"",
					status, actual.getStatus()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a default type of {@code about:blank} or {@code null}.
	 *
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasDefaultType() {
		return satisfiesAnyOf(
				ignore -> hasType("about:blank"),
				ignore -> hasType((URI) null)
		);
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching type.
	 *
	 * @param type the type URI string
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasType(String type) {
		return hasType(URI.create(type));
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching type.
	 *
	 * @param type the type URI
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasType(URI type) {
		isNotNull();

		if (!Objects.equals(type, actual.getType())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Problem Details should have a type of \"%s\" but was \"%s\"",
					type, actual.getType()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching instance.
	 *
	 * @param instance the instance URI string
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasInstance(String instance) {
		return hasInstance(URI.create(instance));
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching instance.
	 *
	 * @param instance the instance URI
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasInstance(URI instance) {
		isNotNull();

		if (!Objects.equals(instance, actual.getInstance())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Problem Details should have an instance of \"%s\" but was \"%s\"",
					instance, actual.getInstance()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching title.
	 *
	 * @param title the problem detail title
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasTitle(String title) {
		isNotNull();

		if (!Objects.equals(title, actual.getTitle())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Problem Details should have a title of %s but was %s",
					title, actual.getTitle()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a title that contains the given char sequence.
	 *
	 * @param sequence the char sequence to be contained
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasTitleContaining(CharSequence sequence) {
		isNotNull();

		return satisfies(it -> Assertions.assertThat(it.getTitle())
				.as("Problem Details should have a title containing \"%s\"", sequence)
				.contains(sequence)
		);
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a matching detail.
	 *
	 * @param detail the problem detail value
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasDetail(String detail) {
		isNotNull();

		if (!Objects.equals(detail, actual.getDetail())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Problem Details should have a detail of %s but was %s",
					detail, actual.getDetail()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a detail that contains the given char sequence.
	 *
	 * @param sequence the char sequence to be contained
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasDetailContaining(CharSequence sequence) {
		isNotNull();

		return satisfies(it -> Assertions.assertThat(it.getDetail())
				.as("Problem Details should have a detail containing \"%s\"", sequence)
				.contains(sequence)
		);
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a property with a matching name and value.
	 *
	 * @param name  the property name
	 * @param value expected property value
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasProperty(String name, Object value) {
		return satisfies(it -> Assertions.assertThat(it.getProperties())
				.as("Problem Details should have a \"%s\" property that has a value of: %s", name, value)
				.containsEntry(name, value)
		);
	}

	/**
	 * Checks if the given {@link ProblemDetail} has a property that satisfies the given requirements
	 * expressed as a {@link ThrowingConsumer}.
	 *
	 * @param name     the property name
	 * @param consumer consumer used to evaluate the property value
	 * @return the problem detail assert instance, never {@literal null}
	 */
	public ProblemDetailAssert hasPropertySatisfying(String name, ThrowingConsumer<Object> consumer) {
		return satisfies(it -> Assertions.assertThat(it.getProperties())
				.as("Problem Details should have a \"%s\" property", name)
				.hasEntrySatisfying(name, consumer));
	}

}
