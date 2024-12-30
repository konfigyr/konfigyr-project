package com.konfigyr.identity.authorization;

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

}
