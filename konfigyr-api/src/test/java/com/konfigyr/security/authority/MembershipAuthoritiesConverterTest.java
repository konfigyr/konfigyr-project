package com.konfigyr.security.authority;

import com.konfigyr.account.Memberships;
import com.konfigyr.test.TestAccounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class MembershipAuthoritiesConverterTest {

	MembershipAuthoritiesConverter converter = MembershipAuthoritiesConverter.getInstance();

	@MethodSource("memberships")
	@DisplayName("should convert memberships to granted authorities")
	@ParameterizedTest(name = "should convert memberships to following authorities: {1}")
	void shouldConvert(Memberships memberships, Collection<String> expected) {
		assertThat(converter.convert(memberships))
				.allMatch(SimpleGrantedAuthority.class::isInstance)
				.extracting(GrantedAuthority::getAuthority)
				.containsAll(expected);
	}

	static Stream<Arguments> memberships() {
		return Stream.of(
				Arguments.of(
						TestAccounts.john().build().memberships(),
						Set.of("konfigyr:admin", "john-doe:admin")
				),
				Arguments.of(
						TestAccounts.jane().build().memberships(),
						Set.of("konfigyr:user")
				),
				Arguments.of(
						Memberships.empty(),
						Set.of()
				)
		);
	}
}