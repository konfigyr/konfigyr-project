package com.konfigyr.identity.authentication;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.support.Avatar;
import com.konfigyr.test.OAuth2AccessTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AccountIdentityTest {

	@Test
	@DisplayName("should create active account identity")
	void shouldCreateIdentityWithActiveStatus() {
		assertThat(AccountIdentities.john().build())
				.returns(EntityId.from(1), AccountIdentity::getId)
				.returns(EntityId.from(1).serialize(), UserDetails::getUsername)
				.returns("john.doe@konfigyr.com", AccountIdentity::getEmail)
				.returns("John Doe", AccountIdentity::getDisplayName)
				.returns(Avatar.generate(EntityId.from(1), "JD"), AccountIdentity::getAvatar)
				.returns("", UserDetails::getPassword)
				.returns(true, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(true, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("john")
				)
				.hasSameHashCodeAs(AccountIdentities.john().build())
				.isEqualTo(AccountIdentities.john().build());
	}

	@Test
	@DisplayName("should create suspended account identity")
	void shouldCreateIdentityWithSuspendedStatus() {
		final var identity = AccountIdentities.jane()
				.status(AccountIdentityStatus.SUSPENDED)
				.build();

		assertThat(identity)
				.returns(EntityId.from(2), AccountIdentity::getId)
				.returns(EntityId.from(2).serialize(), UserDetails::getUsername)
				.returns("jane.doe@konfigyr.com", AccountIdentity::getEmail)
				.returns("Jane Doe", AccountIdentity::getDisplayName)
				.returns(Avatar.generate(EntityId.from(2), "JD"), AccountIdentity::getAvatar)
				.returns("", UserDetails::getPassword)
				.returns(false, UserDetails::isEnabled)
				.returns(false, UserDetails::isAccountNonLocked)
				.returns(true, UserDetails::isAccountNonExpired)
				.returns(false, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("jane")
				);
	}

	@Test
	@DisplayName("should create deactivated account identity")
	void shouldCreateIdentityWithDeactivatedStatus() {
		final var identity = AccountIdentities.jane()
				.status(AccountIdentityStatus.DEACTIVATED)
				.build();

		assertThat(identity)
				.returns(EntityId.from(2), AccountIdentity::getId)
				.returns(EntityId.from(2).serialize(), UserDetails::getUsername)
				.returns("jane.doe@konfigyr.com", AccountIdentity::getEmail)
				.returns("Jane Doe", AccountIdentity::getDisplayName)
				.returns(Avatar.generate(EntityId.from(2), "JD"), AccountIdentity::getAvatar)
				.returns("", UserDetails::getPassword)
				.returns(false, UserDetails::isEnabled)
				.returns(true, UserDetails::isAccountNonLocked)
				.returns(false, UserDetails::isAccountNonExpired)
				.returns(false, UserDetails::isCredentialsNonExpired)
				.satisfies(it -> assertThat(it.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactlyInAnyOrder("jane")
				);
	}

	@Test
	@DisplayName("should create an OIDC identity user")
	void createOidcUser() {
		final var identity = AccountIdentities.jane()
				.status(AccountIdentityStatus.DEACTIVATED)
				.build();

		final Map<String, Object> claims = Map.of(
				IdTokenClaimNames.SUB, identity.getEmail(),
				"email", identity.getEmail()
		);

		final var token = OAuth2AccessTokens.createIdToken("id-token", claims);

		assertThat(new OidcAccountIdentityUser(identity, new DefaultOidcUser(identity.getAuthorities(), token)))
				.returns(identity, OidcAccountIdentityUser::getAccountIdentity)
				.returns(identity.getId(), OidcAccountIdentityUser::getId)
				.returns(identity.getName(), OidcAccountIdentityUser::getName)
				.returns(identity.getAuthorities(), OidcAccountIdentityUser::getAuthorities)
				.returns(token, OidcAccountIdentityUser::getIdToken)
				.returns(claims, OidcAccountIdentityUser::getClaims)
				.returns(claims, OidcAccountIdentityUser::getAttributes)
				.returns(null, OidcAccountIdentityUser::getUserInfo);
	}

	@Test
	@DisplayName("should create an OAuth2 identity user")
	void createOAuthUser() {
		final var identity = AccountIdentities.jane()
				.status(AccountIdentityStatus.DEACTIVATED)
				.build();

		final Map<String, Object> claims = Map.of(
				IdTokenClaimNames.SUB, identity.getEmail(),
				"email", identity.getEmail()
		);

		assertThat(new OAuthAccountIdentityUser(identity, new DefaultOAuth2User(identity.getAuthorities(), claims, "email")))
				.returns(identity, OAuthAccountIdentityUser::getAccountIdentity)
				.returns(identity.getId(), OAuthAccountIdentityUser::getId)
				.returns(identity.getName(), OAuthAccountIdentityUser::getName)
				.returns(identity.getAuthorities(), OAuthAccountIdentityUser::getAuthorities)
				.returns(claims, OAuthAccountIdentityUser::getAttributes);
	}

	@Test
	@DisplayName("account identity should not expose personal data")
	void hidePersonalData() {
		final var identity = AccountIdentities.john().build();

		assertThat(identity)
				.hasToString("AccountIdentity(id=%s, status=%s)", identity.getId(), identity.getStatus());
	}

	@Test
	@DisplayName("should create identity created event")
	void shouldCreateIdentityCreatedEvent() {
		final var identity = AccountIdentities.jane().build();
		final var event = new AccountIdentityEvent.Created(identity);

		assertThat(event)
				.returns(identity, AccountIdentityEvent::identity)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(Instant.now(), within(100, ChronoUnit.MILLIS))
				)
				.hasSameHashCodeAs(event)
				.isNotEqualTo(new AccountIdentityEvent.Created(identity))
				.doesNotHaveSameHashCodeAs(new AccountIdentityEvent.Created(identity))
				.hasToString("AccountIdentityCreated[identity=%s, timestamp=%s]", identity, event.timestamp());
	}

}
