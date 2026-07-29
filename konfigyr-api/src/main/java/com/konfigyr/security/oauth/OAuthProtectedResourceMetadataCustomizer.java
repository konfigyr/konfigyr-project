package com.konfigyr.security.oauth;


import com.konfigyr.security.ResourceServerScopes;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.server.resource.OAuth2ProtectedResourceMetadata;

import java.util.function.Consumer;

/**
 * Customizer that populates the {@link OAuth2ProtectedResourceMetadata} exposed by the Konfigyr
 * REST API on the {@code /.well-known/oauth-protected-resource} endpoint with the resource name,
 * the authorization server issuer and the supported OAuth scopes.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ResourceServerScopes
 */
@RequiredArgsConstructor
public final class OAuthProtectedResourceMetadataCustomizer implements
		Consumer<OAuth2ProtectedResourceMetadata.Builder> {

	private final PropertyMapper mapper = PropertyMapper.get();
	private final OAuth2ResourceServerProperties properties;

	/**
	 * Customizes the given {@link OAuth2ProtectedResourceMetadata.Builder} with the Konfigyr
	 * REST API resource name, the JWT issuer URI as the authorization server and the
	 * {@link ResourceServerScopes supported OAuth scopes}.
	 *
	 * @param builder the protected resource metadata builder to customize, never {@literal null}
	 */
	@Override
	public void accept(OAuth2ProtectedResourceMetadata.Builder builder) {
		builder.resourceName("Konfigyr REST API");

		mapper.from(properties.getJwt().getIssuerUri()).to(builder::authorizationServer);
		builder.scopes(ResourceServerScopes::register);
	}
}
