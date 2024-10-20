package com.konfigyr.jooq;

import com.konfigyr.test.AbstractIntegrationTest;
import org.jooq.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;

class PageableExecutorTest extends AbstractIntegrationTest {

	@Autowired
	DSLContext context;

	PageableExecutor executor;

	@BeforeEach
	void setup() {
		executor = PageableExecutor.builder()
				.defaultSortField(ACCOUNTS.ID, SortOrder.ASC)
				.sortField("id", ACCOUNTS.ID)
				.sortField("email", ACCOUNTS.EMAIL)
				.sortField("date", ACCOUNTS.UPDATED_AT)
				.build();
	}

	@Test
	@DisplayName("should execute pageable")
	void shouldExecutePageable() {
		final var pageable = PageRequest.of(0, 1, Sort.by("email"));

		assertThatObject(execute(pageable))
				.isNotNull()
				.returns(1, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(2, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(1, Page::getNumberOfElements)
				.asInstanceOf(iterable(String.class))
				.hasSize(1)
				.containsExactly("jane.doe@konfigyr.com");

		assertThatObject(execute(pageable.next()))
				.isNotNull()
				.returns(1, Page::getSize)
				.returns(1, Page::getNumber)
				.returns(2, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(1, Page::getNumberOfElements)
				.asInstanceOf(iterable(String.class))
				.hasSize(1)
				.containsExactly("john.doe@konfigyr.com");
	}

	@Test
	@Transactional
	@DisplayName("should execute pageable with multiple sort orders")
	void shouldExecutePageableWithMultipleSortOrders() {
		final var timestamp = LocalDate.now().minusDays(2).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

		context.update(ACCOUNTS)
				.set(ACCOUNTS.UPDATED_AT, timestamp.plusHours(6))
				.where(ACCOUNTS.EMAIL.eq("john.doe@konfigyr.com"))
				.execute();

		context.update(ACCOUNTS)
				.set(ACCOUNTS.UPDATED_AT, timestamp.minusDays(2))
				.where(ACCOUNTS.EMAIL.eq("jane.doe@konfigyr.com"))
				.execute();

		context.insertInto(ACCOUNTS)
				.set(ACCOUNTS.ID, 3L)
				.set(ACCOUNTS.EMAIL, "stilgar@arakis.com")
				.set(ACCOUNTS.STATUS, "ACTIVE")
				.set(ACCOUNTS.UPDATED_AT, timestamp)
				.execute();

		context.insertInto(ACCOUNTS)
				.set(ACCOUNTS.ID, 4L)
				.set(ACCOUNTS.EMAIL, "muad.dib@arakis.com")
				.set(ACCOUNTS.STATUS, "DISABLED")
				.set(ACCOUNTS.UPDATED_AT, timestamp)
				.execute();

		final var pageable = PageRequest.of(0, 100, Sort.by("date", "email"));

		assertThatObject(execute(pageable))
				.isNotNull()
				.returns(100, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(1, Page::getTotalPages)
				.returns(4L, Page::getTotalElements)
				.returns(4, Page::getNumberOfElements)
				.asInstanceOf(iterable(String.class))
				.hasSize(4)
				.containsExactly(
						"jane.doe@konfigyr.com",
						"muad.dib@arakis.com",
						"stilgar@arakis.com",
						"john.doe@konfigyr.com"
				);
	}

	@Test
	@DisplayName("should execute unsorted pageable")
	void shouldExecuteUnsortedPageable() {
		final var pageable = Pageable.ofSize(10);

		assertThatObject(execute(pageable))
				.isNotNull()
				.returns(10, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(1, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(2, Page::getNumberOfElements)
				.asInstanceOf(iterable(String.class))
				.hasSize(2)
				.containsExactly("john.doe@konfigyr.com", "jane.doe@konfigyr.com");
	}

	@Test
	@DisplayName("should execute unpaged pageable")
	void shouldExecuteUnpagedPageable() {
		assertThatObject(execute(Pageable.unpaged()))
				.isNotNull()
				.returns(2, Page::getSize)
				.returns(0, Page::getNumber)
				.returns(1, Page::getTotalPages)
				.returns(2L, Page::getTotalElements)
				.returns(2, Page::getNumberOfElements)
				.asInstanceOf(iterable(String.class))
				.hasSize(2)
				.containsExactly("john.doe@konfigyr.com", "jane.doe@konfigyr.com");
	}

	private Page<String> execute(Pageable pageable) {
		return executor.execute(
				context.select(ACCOUNTS.EMAIL).from(ACCOUNTS),
				record -> record.get(ACCOUNTS.EMAIL),
				pageable,
				() -> context.fetchCount(context.select(ACCOUNTS.EMAIL).from(ACCOUNTS))
		);
	}

}
