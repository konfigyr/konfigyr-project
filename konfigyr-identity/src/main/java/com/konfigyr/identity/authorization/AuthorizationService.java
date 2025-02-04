package com.konfigyr.identity.authorization;

import org.jmolecules.event.annotation.DomainEventPublisher;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

/**
 * Interface that defines how Konfigyr Identity service manages the following OAuth2 objects.
 * <ul>
 *     <li>{@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization OAuth2 Authorizations}</li>
 *     <li>{@link org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent OAuth2 Consents}</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuth2AuthorizationConsentService
 * @see OAuth2AuthorizationService
 * @see org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
 * @see org.springframework.security.oauth2.server.authorization.OAuth2Authorization
 */
public interface AuthorizationService extends OAuth2AuthorizationService, OAuth2AuthorizationConsentService {

	@Override
	@DomainEventPublisher(publishes = "authorization.authorization-stored")
	void save(OAuth2Authorization authorization);

	@Override
	@DomainEventPublisher(publishes = "authorization.authorization-consent-granted")
	void save(OAuth2AuthorizationConsent authorizationConsent);

	@Override
	@DomainEventPublisher(publishes = "authorization.authorization-revoked")
	void remove(OAuth2Authorization authorization);

	@Override
	@DomainEventPublisher(publishes = "authorization.authorization-consent-revoked")
	void remove(OAuth2AuthorizationConsent authorizationConsent);

}
