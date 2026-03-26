import { useCallback, useMemo, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { GroupIcon } from 'lucide-react';
import { useServiceCatalogQuery } from '@konfigyr/hooks';
import { PropertyDeprecation } from '@konfigyr/components/artifactory/property-deprecation';
import { PropertyDescription } from '@konfigyr/components/artifactory/property-description';
import { PropertySchema } from '@konfigyr/components/artifactory/property-schema';
import { PropertyTypeName } from '@konfigyr/components/artifactory/property-type-name';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import { ErrorState } from '@konfigyr/components/error';
import { SearchInputGroup } from '@konfigyr/components/vault/properties/search-input-group';
import {
  Card,
  CardContent,
} from '@konfigyr/components/ui/card';
import {
  Item,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationRange,
} from '@konfigyr/components/ui/pagination';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import {
  ArtifactLabel,
  MissingManifestsDescription,
  ServiceManifestsInstructions,
} from '../messages';

import type { FormEvent, SyntheticEvent } from 'react';
import type {
  Namespace,
  Service,
  ServiceCatalogProperty,
} from '@konfigyr/hooks/types';

const PER_PAGE = 20 as const;

function useFilter(properties: Array<ServiceCatalogProperty>, filter: string = ''): Array<ServiceCatalogProperty> {
  return useMemo(() => {
    const term = filter.trim().toLowerCase();

    return properties
      .map(property => {
        let score = 0;
        const name = property.name.toLowerCase();
        const description = property.description?.toLowerCase() || '';

        if (name === term) {
          score += 100; // exact match, the highest score
        } else if (name.includes(term)) {
          score += 10; // partial match, slightly higher score
        }

        if (description.includes(term)) {
          score += 1; // description partial matches, lowest score
        }

        return { ...property, score };
      })
      .filter(it => it.score > 0)
      .sort((a, b) => b.score - a.score);
  }, [filter, properties]);
}

function SkeletonLoader() {
  return (
    <article data-slot="artifact-skeleton" className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <Skeleton className="w-48 h-4" />
        <Skeleton className="w-32 h-4" />
      </div>
      <Skeleton className="w-64 h-4" />
      <div className="flex items-center gap-2">
        <Skeleton className="w-16 h-3" />
        <Skeleton className="w-58 h-3" />
      </div>
    </article>
  );
}

function PropertyItem({ property }: { property: ServiceCatalogProperty }) {
  return (
    <Item variant="list">
      <ItemContent>
        <ItemTitle>
          <PropertyName value={property.name} />
          <PropertyTypeName value={property.typeName} />
          <PropertyDeprecation deprecation={property.deprecation} />
        </ItemTitle>
        <ItemDescription>
          <PropertyDescription value={property.description} />
        </ItemDescription>

        <div className="text-xs mt-2 space-y-1">
          <p>
            <span className="text-muted-foreground mr-1">
              Default value:
            </span>
            <span className="font-mono text-muted-foreground/120">
              {property.defaultValue || 'N/A'}
            </span>
          </p>
          <p>
            <span className="text-muted-foreground mr-1">
              JSON Schema type:
            </span>
            <PropertySchema value={property.schema} />
          </p>
          <p>
            <span className="text-muted-foreground mr-1">
              <ArtifactLabel />:
            </span>
            <span className="font-mono text-muted-foreground/120">
              {property.artifact}
            </span>
          </p>
        </div>
      </ItemContent>
    </Item>
  );
}

function Properties({ properties, page = 1 }: { properties: Array<ServiceCatalogProperty>, page?: number } ) {
  const slice = useMemo(() => {
    const i = (page - 1) * PER_PAGE;
    return properties.slice(i, i + PER_PAGE);
  }, [properties, page]);

  if (properties.length === 0) {
    return (
      <EmptyState
        icon={<GroupIcon size="2rem" />}
        title={
          <FormattedMessage
            defaultMessage="No matching properties found"
            description="Empty state title used when no artifacts within the service manifest matches the search term."
          />
        }
        description={
          <FormattedMessage
            defaultMessage="We couldn't find any config properties matching that query. Check your syntax or try searching for a different name."
            description="Empty state description used when no artifacts within the service manifest matches the search term."
          />
        }
      />
    );
  }

  return (
    <ItemGroup>
      {slice.map(property => (
        <PropertyItem
          key={`${property.artifact}:${property.name}`}
          property={property}
        />
      ))}
    </ItemGroup>
  );
}

function SearchForm({ term, onTermChange }: { term: string, onTermChange: (term: string) => void }) {
  const onSubmit = useCallback((event: FormEvent) => {
    event.preventDefault();
    event.stopPropagation();
    onTermChange(term);
  }, [term, onTermChange]);

  return (
    <form className="flex items-center flex-1" onSubmit={onSubmit}>
      <SearchInputGroup
        value={term}
        debounce={260}
        className="max-w-sm"
        onChange={onTermChange}
      />
    </form>
  );
}

export function ServiceCatalog({ namespace, service }: { namespace: Namespace, service: Service }) {
  const [page, setPage] = useState(1);
  const [term, setTerm] = useState('');

  const { data: catalog, error, isPending, isError } = useServiceCatalogQuery(namespace.slug, service.slug);

  const properties = useFilter(catalog?.properties || [], term);
  const pages = useMemo(() => Math.ceil(properties.length / PER_PAGE), [properties.length]);

  const onPageChange = useCallback((event: SyntheticEvent, value: number) => {
    event.preventDefault();
    event.stopPropagation();
    setPage(value);
  }, [setPage]);

  const onTermChange = useCallback((value: string) => {
    setTerm(value);
    setPage(1);
  }, [setPage, setTerm]);

  return (
    <>
      <SearchForm term={term} onTermChange={onTermChange} />
      <Card className="border">
        <CardContent>
          {isPending && (
            <SkeletonLoader />
          )}

          {isError && (
            <ErrorState error={error} className="border-none" />
          )}

          {catalog?.properties.length === 0 ? (
            <EmptyState
              icon={<GroupIcon size="2rem" />}
              title={
                <FormattedMessage
                  defaultMessage="No property metadata found"
                  description="Empty state title used when no configuration properties are present in the service manifest."
                />
              }
              description={
                <MissingManifestsDescription />
              }
            >
              <p className="text-muted-foreground text-sm/relaxed">
                <ServiceManifestsInstructions />
              </p>
            </EmptyState>
          ) : (
            <Properties page={page} properties={properties} />
          )}
        </CardContent>
      </Card>

      {pages > 1 && (
        <Pagination page={page} pages={pages} total={catalog?.properties.length} size={PER_PAGE} className="mt-4">
          <PaginationContent>
            <PaginationItem >
              <PaginationPrevious onClick={e => onPageChange(e, page - 1)} />
            </PaginationItem>
            <PaginationRange>
              {state => (
                <PaginationLink isActive={state.active} onClick={e => onPageChange(e, state.page)}>
                  {state.page}
                </PaginationLink>
              )}
            </PaginationRange>
            <PaginationItem>
              <PaginationNext onClick={e => onPageChange(e, page + 1)} />
            </PaginationItem>
          </PaginationContent>
        </Pagination>
      )}
    </>
  );
}
