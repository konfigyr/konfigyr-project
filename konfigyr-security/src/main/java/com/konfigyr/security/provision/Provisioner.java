package com.konfigyr.security.provision;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.account.AccountRegistration;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceDefinition;
import com.konfigyr.namespace.NamespaceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Custom Spring component that would provision the {@link Account} and the {@link Namespace}
 * that was entered by the user in the {@link ProvisioningForm}.
 *
 * @author Vladimir Spasic
 **/
@Slf4j
@Component
@RequiredArgsConstructor
class Provisioner {

	private static final Marker PROVISIONED = MarkerFactory.getMarker("PROVISIONING_SUCCESS");

	private static final UriComponentsBuilder FORWARD_URI = UriComponentsBuilder
			.fromUriString(ProvisioningAuthenticationFilter.DEFAULT_PROCESSING_URL)
			.queryParam("account", "{account}");

	private final AccountManager accountManager;
	private final NamespaceManager namespaceManager;

	@NonNull
	@Transactional
	UriComponents provision(@NonNull ProvisioningForm form) {
		log.debug("Attempting to provision Account and Namespace using: {}", form);

		final Account account = accountManager.create(
				AccountRegistration.builder()
						.email(form.getEmail())
						.firstName(form.getFirstName())
						.lastName(form.getLastName())
						.avatar(form.getAvatar())
						.build()
		);

		final Namespace namespace = namespaceManager.create(
				NamespaceDefinition.builder()
						.name(form.getNamespace())
						.slug(form.getNamespace())
						.type(form.getType())
						.owner(account.id())
						.build()
		);

		log.info(PROVISIONED, "Successfully provisioned [account={}, namespace={}]", account, namespace);

		return FORWARD_URI.buildAndExpand(account.id().serialize());
	}
}
