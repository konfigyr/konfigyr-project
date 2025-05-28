package com.konfigyr.identity.authentication.idenitity;

import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityStatus;
import com.konfigyr.support.Avatar;
import com.konfigyr.support.FullName;
import org.jooq.Record;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

@SuppressWarnings("checkstyle:InterfaceIsType")
interface AccountIdentityMapper {

	List<GrantedAuthority> AUTHORITIES = AuthorityUtils.createAuthorityList("konfigyr-identity");

	/**
	 * Construct the {@link AccountIdentity} from the returned jOOQ record.
	 *
	 * @param record jOQQ record, can't be {@literal null}
	 * @return account identity, never {@link null}
	 */
	@NonNull
	static AccountIdentity map(@NonNull Record record) {
		final EntityId id = record.get(ACCOUNTS.ID, EntityId.class);
		final String email = record.get(ACCOUNTS.EMAIL);

		FullName name = FullName.of(email, null);

		if (record.get(ACCOUNTS.FIRST_NAME) != null && record.get(ACCOUNTS.FIRST_NAME) != null) {
			name = FullName.of(record.get(ACCOUNTS.FIRST_NAME), record.get(ACCOUNTS.LAST_NAME));
		}

		Avatar avatar = record.get(ACCOUNTS.AVATAR, Avatar.class);

		if (avatar == null) {
			avatar = Avatar.generate(id, name.initials());
		}

		return AccountIdentity.builder()
				.id(id)
				.email(email)
				.displayName(name.get())
				.avatar(avatar)
				.status(record.get(ACCOUNTS.STATUS, AccountIdentityStatus.class))
				.authorities(AUTHORITIES)
				.build();
	}

	/**
	 * Attempts to create {@link SettableRecord} from the obtained {@link OAuth2AuthenticatedPrincipal}
	 * and the {@link ClientRegistration}.
	 *
	 * @param user obtained OAuth user attributes, can't be {@code null}
	 * @param client OAuth client used to extract the attributes, can't be {@code null}
	 * @return settable record, never {@link null}
	 */
	@NonNull
	static SettableRecord map(@NonNull OAuth2AuthenticatedPrincipal user, @NonNull ClientRegistration client) {
		return switch (client.getRegistrationId()) {
			case "github" -> github(user);
			case "gitlab" -> gitlab(user);
			default -> openid(user);
		};
	}

	/**
	 * Creates {@link SettableRecord} from the attributes that are be obtained from GitHub User endpoint URL.
	 *
	 * @param user GitHub OAuth user, can't be {@code null}
	 * @return provisioning hints
	 * @see <a href="https://docs.github.com/en/rest/users/users#get-the-authenticated-user">GitHub Docs</a>
	 */
	@NonNull
	private static SettableRecord github(@NonNull OAuth2AuthenticatedPrincipal user) {
		final Optional<FullName> name = extract(user, "name", FullName::parse);

		return SettableRecord.of(ACCOUNTS)
				.set(ACCOUNTS.EMAIL, extract(user, "email"))
				.set(ACCOUNTS.FIRST_NAME, name.map(FullName::firstName))
				.set(ACCOUNTS.LAST_NAME, name.map(FullName::lastName))
				.set(ACCOUNTS.AVATAR, extract(user, "avatar_url", URI::create).map(URI::toString));
	}

	/**
	 * Creates {@link SettableRecord} from the attributes that are be obtained from GitLab User endpoint URL.
	 *
	 * @param user GitLab OAuth user, can't be {@code null}
	 * @return provisioning hints
	 * @see <a href="https://docs.gitlab.com/ee/api/users.html#list-current-user">GitLab Docs</a>
	 */
	private static SettableRecord gitlab(@NonNull OAuth2AuthenticatedPrincipal user) {
		return github(user);
	}

	/**
	 * Creates {@link SettableRecord} from the attributes that are using the OpenID standards.
	 *
	 * @param user OpenID OAuth user, can't be {@code null}
	 * @return provisioning hints
	 * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims">OpenID Claims</a>
	 */
	private static SettableRecord openid(@NonNull OAuth2AuthenticatedPrincipal user) {
		final Optional<FullName> name = extract(user, "name", FullName::parse);

		return SettableRecord.of(ACCOUNTS)
				.set(ACCOUNTS.EMAIL, extract(user, "email"))
				.set(ACCOUNTS.FIRST_NAME, name.map(FullName::firstName))
				.set(ACCOUNTS.LAST_NAME, name.map(FullName::lastName))
				.set(ACCOUNTS.AVATAR, extract(user, "picture", URI::create).map(URI::toString));
	}

	private static <T> Optional<T> extract(OAuth2AuthenticatedPrincipal user, String attribute) {
		return extract(user, attribute, Function.identity());
	}

	private static <T, R> Optional<R> extract(OAuth2AuthenticatedPrincipal user, String attribute, Function<T, R> converter) {
		final T value = user.getAttribute(attribute);

		if (value == null) {
			return Optional.empty();
		}

		try {
			return Optional.ofNullable(converter.apply(value));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
