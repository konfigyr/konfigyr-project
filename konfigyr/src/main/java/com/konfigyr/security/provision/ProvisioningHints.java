package com.konfigyr.security.provision;

import com.konfigyr.namespace.NamespaceType;
import com.konfigyr.support.FullName;
import com.konfigyr.support.Slug;
import lombok.Builder;
import lombok.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.function.Function;

/**
 * Immutable value object that contains hints that can be used to pre-fill the account provisioning form.
 * <p>
 * The values are usually coming from an external identity provider that may expose user attributes,
 * usually via {@link OAuth2AuthenticatedPrincipal} type.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProvisioningRequiredException
 **/
@Value
@Builder
public class ProvisioningHints implements Serializable {

	@Serial
	private static final long serialVersionUID = 8287276943089219363L;

	static final ProvisioningHints EMPTY = ProvisioningHints.builder().build();

	@Nullable String email;
	@Nullable FullName name;
	@Nullable URI avatar;
	@Nullable Slug namespace;
	@Nullable NamespaceType type;

	/**
	 * Attempts to create {@link ProvisioningHints} from the obtained {@link OAuth2AuthenticatedPrincipal}.
	 *
	 * @param user obtained OAuth user attributes, can't be {@code null}
	 * @param provider provider used to extract the attributes, can't be {@code null}
	 * @return provisioning hints
	 */
	public static ProvisioningHints from(@NonNull OAuth2AuthenticatedPrincipal user, @NonNull String provider) {
		return switch (provider) {
			case "github" -> github(user);
			case "gitlab" -> gitlab(user);
			default -> openid(user);
		};
	}

	/**
	 * Creates {@link ProvisioningHints} from the attributes that are be obtained from GitHub User endpoint URL.
	 *
	 * @param user GitHub OAuth user, can't be {@code null}
	 * @return provisioning hints
	 * @see <a href="https://docs.github.com/en/rest/users/users#get-the-authenticated-user">GitHub Docs</a>
	 */
	@NonNull
	public static ProvisioningHints github(@NonNull OAuth2AuthenticatedPrincipal user) {
		return ProvisioningHints.builder()
				.email(extract(user, "email"))
				.name(extract(user, "name", FullName::parse))
				.avatar(extract(user, "avatar_url", URI::create))
				.namespace(extract(user, "login", Slug::slugify))
				.type(NamespaceType.PERSONAL)
				.build();
	}

	/**
	 * Creates {@link ProvisioningHints} from the attributes that are be obtained from GitLab User endpoint URL.
	 *
	 * @param user GitLab OAuth user, can't be {@code null}
	 * @return provisioning hints
	 * @see <a href="https://docs.gitlab.com/ee/api/users.html#list-current-user">GitLab Docs</a>
	 */
	public static ProvisioningHints gitlab(@NonNull OAuth2AuthenticatedPrincipal user) {
		return ProvisioningHints.builder()
				.email(extract(user, "email"))
				.name(extract(user, "name", FullName::parse))
				.avatar(extract(user, "avatar_url", URI::create))
				.namespace(extract(user, "username", Slug::slugify))
				.type(NamespaceType.PERSONAL)
				.build();
	}

	/**
	 * Creates {@link ProvisioningHints} from the attributes that are using the OpenID standards.
	 *
	 * @param user OpenID OAuth user, can't be {@code null}
	 * @return provisioning hints
	 * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims">OpenID Claims</a>
	 */
	public static ProvisioningHints openid(@NonNull OAuth2AuthenticatedPrincipal user) {
		return ProvisioningHints.builder()
				.email(extract(user, "email"))
				.name(extract(user, "name", FullName::parse))
				.avatar(extract(user, "picture", URI::create))
				.namespace(extract(user, "preferred_username", Slug::slugify))
				.type(NamespaceType.PERSONAL)
				.build();
	}

	private static <T> T extract(OAuth2AuthenticatedPrincipal user, String attribute) {
		return extract(user, attribute, Function.identity());
	}

	private static <T, R> R extract(OAuth2AuthenticatedPrincipal user, String attribute, Function<T, R> converter) {
		final T value = user.getAttribute(attribute);

		if (value == null) {
			return null;
		}

		try {
			return converter.apply(value);
		} catch (Exception e) {
			return null;
		}
	}
}
