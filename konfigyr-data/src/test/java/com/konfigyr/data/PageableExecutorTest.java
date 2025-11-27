package com.konfigyr.data;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.jooq.*;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

import static com.konfigyr.data.TestingTable.TESTING_TABLE;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;

@TestProfile
@ImportTestcontainers(TestContainers.class)
@SpringBootTest(classes = PageableExecutorTest.Config.class)
class PageableExecutorTest {

	@Autowired
	DSLContext context;

	PageableExecutor executor;

	@BeforeEach
	void setup() {
		executor = PageableExecutor.builder()
				.defaultSortField(TESTING_TABLE.ID, SortOrder.DESC)
				.sortField("id", TESTING_TABLE.ID)
				.sortField("state", TESTING_TABLE.STATUS)
				.sortField("date", TESTING_TABLE.TIMESTAMP)
				.build();

		insertTestData(1, "ACTIVE", OffsetDateTime.now(ZoneOffset.UTC));
		insertTestData(2, "INACTIVE", OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(30));
	}

	@AfterEach
	void cleanup() {
		context.deleteFrom(TESTING_TABLE).execute();
	}

	@Test
	@DisplayName("should execute pageable")
	void shouldExecutePageable() {
		final var pageable = PageRequest.of(0, 1, Sort.by("state"));

		assertThatObject(execute(pageable))
				.isNotNull()
				.returns(1, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(2, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(1, Page::getNumberOfElements)
				.asInstanceOf(iterable(Long.class))
				.hasSize(1)
				.containsExactly(1L);

		assertThatObject(execute(pageable.next()))
				.isNotNull()
				.returns(1, Page::getSize)
				.returns(1, Page::getNumber)
				.returns(2, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(1, Page::getNumberOfElements)
				.asInstanceOf(iterable(Long.class))
				.hasSize(1)
				.containsExactly(2L);
	}

	@Test
	@Transactional
	@DisplayName("should execute pageable with multiple sort orders")
	void shouldExecutePageableWithMultipleSortOrders() {
		final var timestamp = LocalDate.now().minusDays(2)
				.atStartOfDay(ZoneOffset.UTC)
				.toOffsetDateTime();

		insertTestData(3, "DISABLED", timestamp);
		insertTestData(4, "ACTIVE", timestamp);
		insertTestData(5, "DISABLED", timestamp.plusSeconds(60));
		insertTestData(6, "INACTIVE", timestamp.minusSeconds(60));

		final var pageable = PageRequest.of(0, 100, Sort.by("date", "state"));

		assertThatObject(execute(pageable))
				.isNotNull()
				.returns(100, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(1, Page::getTotalPages)
				.returns(6L, Page::getTotalElements)
				.returns(6, Page::getNumberOfElements)
				.asInstanceOf(iterable(Long.class))
				.hasSize(6)
				.containsExactly(6L, 4L, 3L, 5L, 2L, 1L);
	}

	@Test
	@DisplayName("should execute unsorted pageable and apply default sort")
	void shouldExecuteUnsortedPageable() {
		final var pageable = Pageable.ofSize(10);

		assertThatObject(execute(pageable))
				.isNotNull()
				.returns(10, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(1, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(2, Page::getNumberOfElements)
				.asInstanceOf(iterable(Long.class))
				.hasSize(2)
				.containsExactly(2L, 1L);
	}

	@Test
	@DisplayName("should execute unpaged pageable and apply default sort")
	void shouldExecuteUnpagedPageable() {
		assertThatObject(execute(Pageable.unpaged()))
				.isNotNull()
				.returns(2, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(1, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(2, Page::getNumberOfElements)
				.asInstanceOf(iterable(Long.class))
				.hasSize(2)
				.containsExactly(2L, 1L);
	}

	@Test
	@DisplayName("should execute unpaged pageable with timestamp sort")
	void shouldExecuteUnpagedPageableWithTimestampSort() {
		final var pageable = Pageable.unpaged(Sort.by(Sort.Direction.DESC, "date"));

		assertThatObject(execute(pageable))
				.isNotNull()
				.returns(2, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(1, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(2, Page::getNumberOfElements)
				.asInstanceOf(iterable(Long.class))
				.hasSize(2)
				.containsExactly(1L, 2L);
	}

	private Page<@NonNull Long> execute(Pageable pageable) {
		return executor.execute(
				context.select(TESTING_TABLE.fields()).from(TESTING_TABLE),
				record -> record.get(TESTING_TABLE.ID),
				pageable,
				() -> context.fetchCount(context.select(TESTING_TABLE.ID).from(TESTING_TABLE))
		);
	}

	private void insertTestData(long id, String state, OffsetDateTime timestamp) {
		final var record = SettableRecord.of(TESTING_TABLE)
				.set(TESTING_TABLE.ID, id)
				.set(TESTING_TABLE.STATUS, state)
				.set(TESTING_TABLE.TIMESTAMP, timestamp);

		context.insertInto(TESTING_TABLE)
				.set(record.get())
				.execute();
	}

	@EnableAutoConfiguration
	static class Config {

	}

}
