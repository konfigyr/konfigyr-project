package com.konfigyr.hateoas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LinkTest {

	@Test
	@DisplayName("link created with href only should be self links")
	void linkWithHrefOnlyBecomesSelfLink() {
		assertThat(Link.of("foo"))
				.returns(LinkRelation.SELF, Link::rel)
				.returns("foo", Link::href)
				.matches(link -> link.hasRel(LinkRelation.SELF));
	}

	@Test
	@DisplayName("should create links with href and relation")
	void createsLinkFromRelAndHref() {
		assertThat(Link.of("foo", LinkRelation.HELP))
				.returns(LinkRelation.HELP, Link::rel)
				.returns("foo", Link::href)
				.matches(link -> link.hasRel(LinkRelation.HELP));
	}

	@Test
	@DisplayName("should fail to create link with null href")
	void rejectsNullHref() {
		assertThatIllegalArgumentException().isThrownBy(() -> Link.of(null));
	}

	@Test
	@DisplayName("should fail to create link with null reference")
	void rejectsNullRel() {
		assertThatIllegalArgumentException().isThrownBy(() -> Link.of("foo", (String) null));
	}

	@Test
	@DisplayName("should fail to create link with empty href")
	void rejectsEmptyHref() {
		assertThatIllegalArgumentException().isThrownBy(() -> Link.of(""));
	}

	@Test
	@DisplayName("should fail to create link with empty reference")
	void rejectsEmptyRel() {
		assertThatIllegalArgumentException().isThrownBy(() -> Link.of("foo", ""));
	}

	@Test
	@DisplayName("links with same href and relations are considered equals")
	void sameRelAndHrefMakeSameLink() {
		Link left = Link.of("foo", LinkRelation.SELF);
		Link right = Link.of("foo", LinkRelation.SELF);

		assertThat(left)
				.isEqualTo(right)
				.hasSameHashCodeAs(right);
	}

	@Test
	@DisplayName("links with same href but different relations are not considered equals")
	void differentRelMakesDifferentLink() {
		Link left = Link.of("foo", LinkRelation.PREV);
		Link right = Link.of("foo", LinkRelation.NEXT);

		assertThat(left)
				.isNotEqualTo(right)
				.doesNotHaveSameHashCodeAs(right);
	}

	@Test
	@DisplayName("links with same relations but different hrefs are not considered equals")
	void differentHrefMakesDifferentLink() {
		Link left = Link.of("foo", LinkRelation.SELF);
		Link right = Link.of("bar", LinkRelation.SELF);

		assertThat(left)
				.isNotEqualTo(right)
				.doesNotHaveSameHashCodeAs(right);
	}

	@Test
	@DisplayName("should check if link is of relation")
	void exposesLinkRelation() {
		assertThat(Link.of("/", LinkRelation.SELF))
				.matches(link -> link.hasRel(LinkRelation.SELF))
				.matches(link -> link.hasRel(LinkRelation.SELF.get()))
				.matches(link -> !link.hasRel(LinkRelation.SEARCH))
				.matches(link -> !link.hasRel(LinkRelation.SEARCH.get()));
	}

	@Test
	@DisplayName("should fail to check if link is of relation when relation is null or empty")
	void rejectsInvalidRelationsOnHasRel() {
		Link link = Link.of("/");

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> link.hasRel((String) null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> link.hasRel(""));
	}

}
