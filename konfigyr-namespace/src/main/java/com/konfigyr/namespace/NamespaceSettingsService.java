package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@Service
@RequiredArgsConstructor
public class NamespaceSettingsService {

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	@Transactional
	@CacheEvict(value = DefaultNamespaceManager.CACHE_NAME, key = "#namespace")
	public void name(String namespace, String name) {
		Assert.notNull(namespace, "Namespace identifier can not be null");
		Assert.hasText(name, "Namespace name can not be null");

		context.update(NAMESPACES)
				.set(NAMESPACES.NAME, name)
				.set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
				.where(NAMESPACES.SLUG.eq(namespace))
				.execute();
	}

	@Transactional
	@CacheEvict(value = DefaultNamespaceManager.CACHE_NAME, key = "#namespace")
	public void slug(String namespace, Slug slug) {
		Assert.notNull(namespace, "Namespace identifier can not be null");
		Assert.notNull(slug, "Namespace slug can not be null");

		final EntityId id = context.update(NAMESPACES)
				.set(NAMESPACES.SLUG, slug.get())
				.set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
				.where(NAMESPACES.SLUG.eq(namespace))
				.returning(NAMESPACES.ID)
				.fetchOptional(NAMESPACES.ID)
				.map(EntityId::from)
				.orElseThrow(() -> new NamespaceNotFoundException(namespace));

		publisher.publishEvent(new NamespaceEvent.Renamed(id, Slug.slugify(namespace), slug));
	}

	@Transactional
	@CacheEvict(value = DefaultNamespaceManager.CACHE_NAME, key = "#namespace")
	public void description(String namespace, String description) {
		Assert.notNull(namespace, "Namespace identifier can not be null");
		Assert.notNull(namespace, "Namespace identifier can not be null");

		context.update(NAMESPACES)
				.set(NAMESPACES.DESCRIPTION, StringUtils.hasText(description) ? description : null)
				.set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
				.where(NAMESPACES.SLUG.eq(namespace))
				.execute();

	}

}
