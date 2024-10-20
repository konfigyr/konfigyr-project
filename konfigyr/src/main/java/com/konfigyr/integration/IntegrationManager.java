package com.konfigyr.integration;

import com.konfigyr.entity.EntityId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface IntegrationManager {

	@NonNull
	Page<Integration> find(@NonNull EntityId namespace, @NonNull Pageable pageable);

	@NonNull
	Page<Integration> find(@NonNull String namespace, @NonNull Pageable pageable);

	@NonNull
	Optional<Integration> get(@NonNull EntityId namespace, @NonNull EntityId id);

	@NonNull
	Optional<Integration> get(@NonNull String namespace, @NonNull EntityId id);

}
