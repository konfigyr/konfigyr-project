package com.konfigyr.identity.authorization.controller;

import com.konfigyr.identity.KonfigyrIdentityRequestMatchers;
import com.konfigyr.identity.authorization.AuthorizationServerScopes;
import com.konfigyr.security.OAuthScope;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping(KonfigyrIdentityRequestMatchers.SCOPES_METADATA_PAGE)
class AuthorizationScopesMetadataController {

	private final MessageSourceAccessor messageSourceAccessor;

	AuthorizationScopesMetadataController(MessageSource messageSource) {
		this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
	}

	@GetMapping
	public Set<ScopeMetadata> metadata() {
		final Set<ScopeMetadata> scopes = new LinkedHashSet<>();

		AuthorizationServerScopes.get().forEach(scope -> {
			scopes.add(ScopeMetadata.from(scope, messageSourceAccessor));

			scope.getIncluded().forEach(included -> scopes.add(
					ScopeMetadata.from(included, messageSourceAccessor)
			));
		});

		return scopes;
	}

	record ScopeMetadata(String name, String description) {
		static ScopeMetadata from(OAuthScope scope, MessageSourceAccessor accessor) {
			String description;

			try {
				description = accessor.getMessage("konfigyr.oauth.scope." + scope.name());
			} catch (NoSuchMessageException ex) {
				description = null;
			}

			return new ScopeMetadata(scope.getAuthority(), description);
		}
	}

}
