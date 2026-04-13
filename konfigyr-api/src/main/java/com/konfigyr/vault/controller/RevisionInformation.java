package com.konfigyr.vault.controller;

import com.konfigyr.vault.ApplyResult;

import java.time.OffsetDateTime;

record RevisionInformation(
		String revision,
		String author,
		String subject,
		String description,
		OffsetDateTime timestamp
) {
	RevisionInformation(ApplyResult result) {
		this(result.revision(), result.author().getDisplayName().orElseGet(result.author()),
				result.subject(), result.description(), result.timestamp());
	}
}
