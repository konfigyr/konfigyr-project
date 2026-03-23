package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.Manifest;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.support.SearchQuery;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;

/**
 * Interface that is used to retrieve or query the service catalog.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ServiceCatalog
 */
@NullMarked
public interface ServiceCatalogSource {

	/**
	 * Returns the complete configuration catalog of the specified {@link Service}.
	 * <p>
	 * The returned {@link ServiceCatalog} represents the materialized set of configuration
	 * property descriptors available to the service. The catalog is derived from the
	 * service's {@link Manifest} and contains property definitions contributed by all
	 * artifacts that participate in the service release.
	 * <p>
	 * Consumers may use the catalog to perform local discovery of configuration properties,
	 * implement autocomplete functionality, or merge property descriptors with the current
	 * configuration state.
	 *
	 * @param service configuration catalog owner, must not be {@literal null}
	 * @return the configuration catalog of the service; never {@literal null}
	 */
	ServiceCatalog get(Service service);

	/**
	 * Searches the configuration catalog of the specified {@link Service} and returns a page of
	 * {@link PropertyDescriptor property descriptors} matching the provided {@link SearchQuery}.
	 * <p>
	 * The search operates exclusively within the service's configuration catalog and returns
	 * descriptors originating from artifacts referenced in the service's current {@link Manifest}.
	 *
	 * @param service configuration catalog owner, must not be {@literal null}
	 * @param query the search query describing filtering and pagination parameters, must not be {@literal null}
	 * @return descriptors that match the query; never {@literal null} but may be empty
	 */
	Page<PropertyDescriptor> query(Service service, SearchQuery query);

}
