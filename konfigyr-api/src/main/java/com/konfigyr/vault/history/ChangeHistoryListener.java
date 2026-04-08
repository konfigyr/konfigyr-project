package com.konfigyr.vault.history;

import com.konfigyr.vault.VaultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ChangeHistoryListener {

	private final ChangeHistoryService service;

	@Async
	@EventListener(id = "vault-change-history-listener", value = VaultEvent.ChangesApplied.class)
	void createChangeHistory(VaultEvent.ChangesApplied event) {
		service.commit(event.id(), event.result());
	}

}
