package com.konfigyr.security.oauth;

import com.konfigyr.security.ResourceServerScopes;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.server.resource.OAuth2ProtectedResourceMetadata;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class OAuthProtectedResourceCustomizer implements Consumer<OAuth2ProtectedResourceMetadata.Builder> {

	private final PropertyMapper mapper = PropertyMapper.get();
	private final OAuth2ResourceServerProperties properties;

	@Override
	public void accept(OAuth2ProtectedResourceMetadata.Builder builder) {
		builder.resourceName("Konfigyr REST API");

		mapper.from(properties.getJwt().getIssuerUri()).to(builder::authorizationServer);
		builder.scopes(ResourceServerScopes::register);
	}
}
