package com.konfigyr.hateoas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

class RepresentationModelTest {

	@Test
	@DisplayName("should create empty representation model")
	void setsUpWithEmptyLinkList() {
		RepresentationModel<?> support = new RepresentationModel<>();

		assertThat(support.hasLinks())
				.isFalse();
		assertThat(support.getLinks())
				.isEmpty();
		assertThat(support.getLinks(LinkRelation.SELF))
				.isEmpty();
	}

	@Test
	@DisplayName("should add single link to representation model")
	void addsLinkCorrectly() {
		final Link link = Link.of("/next-link", LinkRelation.NEXT.get());
		RepresentationModel<?> support = new RepresentationModel<>(link);

		assertThat(support.hasLinks())
				.isTrue();
		assertThat(support.getLinks())
				.containsExactly(link);
		assertThat(support.getLink(LinkRelation.NEXT))
				.hasValue(link);
	}

	@Test
	@DisplayName("should add multiple links to representation model")
	void addsMultipleLinkRelationsCorrectly() {
		Link first = Link.of("/search?page=1", LinkRelation.FIRST);
		Link last = Link.of("/search?page=10", LinkRelation.LAST);
		RepresentationModel<?> support = new RepresentationModel<>(first, last);

		assertThat(support.getLinks())
				.hasSize(2)
				.containsExactly(first, last);

		assertThat(support.getLinks(LinkRelation.FIRST.get()))
				.hasSize(1)
				.containsExactly(first);

		assertThat(support.getLinks(LinkRelation.LAST.get()))
				.hasSize(1)
				.containsExactly(last);
	}

	@Test
	@DisplayName("should not add null link to representation model")
	void preventsNullLinkBeingAdded() {
		RepresentationModel<?> support = new RepresentationModel<>();

		assertThatException().isThrownBy(() -> support.add((Link) null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should not add null links to representation model")
	void preventsNullLinksBeingAdded() {
		RepresentationModel<?> support = new RepresentationModel<>();

		assertThatException().isThrownBy(() -> support.add((Link) null))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatException().isThrownBy(() -> support.add((Link[]) null))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatException().isThrownBy(() -> support.add((Iterable<Link>) null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should add links to representation model")
	void addLinksCorrectly() {
		RepresentationModel<?> support = new RepresentationModel<>();

		support.add(Link.of("/self-link"));
		support.add(
				Link.of("/next-link", LinkRelation.NEXT),
				Link.of("/previous-link", LinkRelation.PREVIOUS)
		);
		support.add(List.of(
				Link.of("/first-link", LinkRelation.FIRST),
				Link.of("/last-link", LinkRelation.LAST)
		));

		assertThat(support.getLinks())
				.hasSize(5);
	}

	@Test
	@DisplayName("should get links from representation model")
	void getLinksCorrectly() {
		RepresentationModel<?> support = new RepresentationModel<>(
				Link.of("/related-link", LinkRelation.RELATED),
				Link.of("/another-related-link", LinkRelation.RELATED)
		);

		assertThat(support.getLink(LinkRelation.SELF.get()))
				.isEmpty();

		assertThat(support.getLinks(LinkRelation.SELF.get()))
				.isEmpty();

		assertThat(support.getLink(LinkRelation.RELATED.get()))
				.hasValue(Link.of("/related-link", LinkRelation.RELATED));

		assertThat(support.getLinks(LinkRelation.RELATED.get()))
				.isUnmodifiable()
				.containsExactly(
						Link.of("/related-link", LinkRelation.RELATED),
						Link.of("/another-related-link", LinkRelation.RELATED)
				);
	}

	@Test
	@DisplayName("should not get links with null relation from representation model")
	void getLinkWithNullRelation() {
		RepresentationModel<?> support = new RepresentationModel<>();

		assertThatException().isThrownBy(() -> support.getLink((String) null))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatException().isThrownBy(() -> support.getLink((LinkRelation) null))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatException().isThrownBy(() -> support.getLinks((String) null))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatException().isThrownBy(() -> support.getLinks((LinkRelation) null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should clear links from representation model")
	void clearsLinksCorrectly() {
		RepresentationModel<?> support = new RepresentationModel<>(Link.of("/self-link"));

		assertThat(support.hasLinks())
				.isTrue();

		assertThat(support.removeLinks().hasLinks())
				.isFalse();
	}

	@Test
	@DisplayName("representation models with same links should be considered equal")
	void sameLinkListMeansSameResource() {
		RepresentationModel<?> first = new RepresentationModel<>();
		RepresentationModel<?> second = new RepresentationModel<>();

		assertThat(first)
				.isEqualTo(second)
				.hasSameHashCodeAs(second);

		final Link link = Link.of("foo");
		first.add(link);
		second.add(link);

		assertThat(first)
				.isEqualTo(second)
				.hasSameHashCodeAs(second);
	}

	@Test
	@DisplayName("representation models with different links should not be considered equal")
	void differentLinkListsNotEqual() {
		RepresentationModel<?> first = new RepresentationModel<>();
		RepresentationModel<?> second = new RepresentationModel<>(Link.of("foo"));

		assertThat(first)
				.isNotEqualTo(second)
				.doesNotHaveSameHashCodeAs(second);
	}

	@Test
	@DisplayName("should create empty representation model from null value")
	void createEmptyModel() {
		RepresentationModel<?> support = RepresentationModel.of(null);

		assertThat(support)
				.isNotNull()
				.isInstanceOf(RepresentationModel.class)
				.returns(false, RepresentationModel::hasLinks);
	}

	@Test
	@DisplayName("should create entity model from single value")
	void createEntityModel() {
		RepresentationModel<?> support = RepresentationModel.of("foo", Link.of("localhost"));

		assertThat(support)
				.isNotNull()
				.isInstanceOf(EntityModel.class);

		assertThat(support.getLink(LinkRelation.SELF))
				.isNotEmpty();
	}

	@Test
	@DisplayName("should create collection model from set of values")
	void createCollectionModel() {
		RepresentationModel<?> support = RepresentationModel.of(Set.of("foo"), Link.of("localhost"));

		assertThat(support)
				.isNotNull()
				.isInstanceOf(CollectionModel.class);
	}

	@Test
	@DisplayName("should create paged model from page result")
	void createPagedModel() {
		RepresentationModel<?> support = RepresentationModel.of(Page.empty(), Link.of("localhost"));

		assertThat(support)
				.isNotNull()
				.isInstanceOf(PagedModel.class);
	}

}
