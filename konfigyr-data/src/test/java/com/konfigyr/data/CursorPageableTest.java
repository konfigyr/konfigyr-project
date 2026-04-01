package com.konfigyr.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class CursorPageableTest {

	@Test
	@DisplayName("should create an unpaged cursor pageable")
	void createUnpagedPageable() {
		final var unpaged = CursorPageable.unpaged();

		assertThat(unpaged)
				.isSameAs(UnpagedCursorPageable.INSTANCE)
				.returns(false, CursorPageable::isPaged)
				.returns(true, CursorPageable::isUnpaged)
				.hasToString("CursorPageable(unpaged)");

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(unpaged::size)
				.withMessage("Can not get size of unpaged cursor pageable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(unpaged::token)
				.withMessage("Can not get token of unpaged cursor pageable");
	}

	@Test
	@DisplayName("should create an cursor pageable without the continuation token")
	void createPageableWithoutToken() {
		assertThat(CursorPageable.of(100))
				.returns(100, CursorPageable::size)
				.returns(null, CursorPageable::token)
				.returns(true, CursorPageable::isPaged)
				.returns(false, CursorPageable::isUnpaged)
				.hasToString("CursorPageable(token=null, size=100)");
	}

	@Test
	@DisplayName("should create an cursor pageable with the continuation token")
	void createPageableWithToken() {
		assertThat(CursorPageable.of("the-token", 10))
				.returns(10, CursorPageable::size)
				.returns("the-token", CursorPageable::token)
				.returns(true, CursorPageable::isPaged)
				.returns(false, CursorPageable::isUnpaged)
				.hasToString("CursorPageable(token=the-token, size=10)");
	}

	@ValueSource(ints = {0, -1})
	@ParameterizedTest(name = "size = {0}")
	@DisplayName("should fail to create cursor pageable with invalid page sizes")
	void createPageableWithInvalidSize(int size) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> CursorPageable.of(size))
				.withMessage("Cursor pageable size must be a positive number: %d", size);
	}

}
