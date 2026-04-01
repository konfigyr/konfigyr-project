package com.konfigyr.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

class CursorPageTest {

	@Test
	@DisplayName("should create an empty cursor page with unspecified paging instructions")
	void createEmptyPageWithoutPagingInstructions() {
		assertThatObject(CursorPage.empty())
				.returns(List.of(), CursorPage::content)
				.returns(0, CursorPage::size)
				.returns(null, CursorPage::nextPageable)
				.returns(null, CursorPage::previousPageable)
				.returns(false, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious)
				.hasToString("CursorSlice(content=[], next=null, previous=null)");
	}

	@Test
	@DisplayName("should create an empty cursor page with next paging instructions")
	void createEmptyPageWithPagingInstructions() {
		final var next = CursorPageable.of(10);

		assertThatObject(CursorPage.empty(next))
				.returns(List.of(), CursorPage::content)
				.returns(0, CursorPage::size)
				.returns(next, CursorPage::nextPageable)
				.returns(null, CursorPage::previousPageable)
				.returns(true, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious)
				.hasToString("CursorSlice(content=[], next=%s, previous=null)", next);
	}

	@Test
	@DisplayName("should create an cursor page with next paging instructions")
	void createPageWithNextPagingInstructions() {
		final var content = List.of("one", "two", "three");
		final var next = CursorPageable.of("next", 3);

		assertThatObject(CursorPage.of(content, next))
				.returns(content, CursorPage::content)
				.returns(3, CursorPage::size)
				.returns(next, CursorPage::nextPageable)
				.returns(null, CursorPage::previousPageable)
				.returns(true, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious)
				.hasToString("CursorSlice(content=%s, next=%s, previous=null)", content, next);
	}

	@Test
	@DisplayName("should create an cursor page with previous paging instructions")
	void createPageWithPreviousPagingInstructions() {
		final var content = List.of("one", "two", "three");
		final var previous = CursorPageable.of(3);

		assertThatObject(CursorPage.of(content, null, previous))
				.returns(content, CursorPage::content)
				.returns(3, CursorPage::size)
				.returns(null, CursorPage::nextPageable)
				.returns(previous, CursorPage::previousPageable)
				.returns(false, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious)
				.hasToString("CursorSlice(content=%s, next=null, previous=%s)", content, previous);
	}

	@Test
	@DisplayName("should create an cursor page with next and previous paging instructions")
	void createPageWithPagingInstructions() {
		final var content = List.of("one", "two", "three");
		final var next = CursorPageable.of("next", 3);
		final var previous = CursorPageable.of("previous", 3);

		assertThatObject(CursorPage.of(content, next, previous))
				.returns(content, CursorPage::content)
				.returns(3, CursorPage::size)
				.returns(next, CursorPage::nextPageable)
				.returns(previous, CursorPage::previousPageable)
				.returns(true, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious)
				.hasToString("CursorSlice(content=%s, next=%s, previous=%s)", content, next, previous);
	}

	@Test
	@DisplayName("should iterate through the cursor page contents")
	void iteratePageContents() {
		final var page = CursorPage.of(List.of("one", "two", "three"));

		assertThat(page)
				.hasSize(3)
				.containsExactly("one", "two", "three")
				.containsExactlyInAnyOrderElementsOf(page.content());

		assertThat(page.content())
				.isUnmodifiable();
	}

	@Test
	@DisplayName("should wrap the page contents into an unmodifiable list")
	void unmodifiablePageContents() {
		final var contents = new ArrayList<String>();
		contents.add("one");
		contents.add("two");
		contents.add("three");

		assertThat(CursorPage.of(contents).content())
				.isUnmodifiable();
	}

}
