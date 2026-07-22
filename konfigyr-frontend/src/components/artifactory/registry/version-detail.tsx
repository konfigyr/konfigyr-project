import { useCallback, useState } from 'react';
import { FormattedDate, useIntl } from 'react-intl';
import { SearchIcon, TagIcon } from 'lucide-react';
import { useDebouncedCallback } from 'use-debounce';
import { useSearchArtifactProperties } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { PropertyDeprecation } from '@konfigyr/components/artifactory/property-deprecation';
import { PropertyDescription } from '@konfigyr/components/artifactory/property-description';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import { PropertySchema } from '@konfigyr/components/artifactory/property-schema';
import { PropertyDefaultValue } from '@konfigyr/components/artifactory/property-default-value';
import { PropertyTypeName } from '@konfigyr/components/artifactory/property-type-name';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { InputGroup, InputGroupAddon, InputGroupInput } from '@konfigyr/components/ui/input-group';
import { Item, ItemContent, ItemGroup, ItemTitle } from '@konfigyr/components/ui/item';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { ArtifactVisibilityBadge } from '@konfigyr/components/artifactory/registry/visibility-badge';
import {
  NoMatchingPropertiesTitle,
  PublishedAtLabel,
  SearchPropertiesPromptDescription,
  SearchPropertiesPromptTitle,
} from '@konfigyr/components/artifactory/registry/messages';

import type { ChangeEvent } from 'react';
import type { PropertyDescriptor, VersionedArtifact } from '@konfigyr/hooks/artifactory/types';

function PropertyItem({ property }: { property: PropertyDescriptor }) {
  return (
    <Item variant="list">
      <ItemContent>
        <ItemTitle>
          <PropertyName value={property.name}/>
          <PropertyTypeName value={property.typeName}/>
          <PropertyDeprecation deprecation={property.deprecation}/>
        </ItemTitle>
        <PropertyDescription value={property.description}/>
        <div className="text-xs mt-2 space-y-1">
          <PropertyDefaultValue variant="labeled" value={property.defaultValue}/>
          <p>
            <span className="text-muted-foreground mr-1">JSON Schema type:</span>
            <PropertySchema value={property.schema}/>
          </p>
        </div>
      </ItemContent>
    </Item>
  );
}

function PropertySearchForm({ term, onTermChange }: { term: string; onTermChange: (term: string) => void }) {
  const intl = useIntl();
  const debounced = useDebouncedCallback((value: string) => onTermChange(value), 260);

  const onChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    debounced(event.target.value);
  }, [debounced]);

  return (
    <InputGroup className="max-w-sm">
      <InputGroupInput
        type="search"
        defaultValue={term}
        placeholder={intl.formatMessage({
          defaultMessage: 'Search properties...',
          description: 'Placeholder for the version property search input.',
        })}
        aria-label={intl.formatMessage({
          defaultMessage: 'Search properties in this version',
          description: 'Aria label for the version property search input.',
        })}
        onChange={onChange}
      />
      <InputGroupAddon>
        <SearchIcon size="1rem" className="text-muted-foreground"/>
      </InputGroupAddon>
    </InputGroup>
  );
}

export function VersionDetail({ namespace, version }: { namespace: string; version: VersionedArtifact }) {
  const [term, setTerm] = useState('');

  const { data, error, isPending, isError } = useSearchArtifactProperties(namespace, {
    groupId: version.groupId,
    artifactId: version.artifactId,
    version: version.version,
    term,
  });

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border bg-card">
            <TagIcon className="size-5 text-muted-foreground" aria-hidden="true"/>
          </div>
          <div className="min-w-0">
            <h1 className="truncate font-mono text-2xl font-medium leading-tight">
              {version.groupId}:{version.artifactId}:{version.version}
            </h1>
            <p className="text-sm text-muted-foreground">
              <PublishedAtLabel/>:{' '}
              <time dateTime={version.publishedAt}>
                <FormattedDate value={version.publishedAt} day="2-digit" month="short" year="numeric"/>
              </time>
            </p>
          </div>
        </div>
        <ArtifactVisibilityBadge
          size="lg"
          visibility={version.visibility}
        />
      </div>

      <PropertySearchForm term={term} onTermChange={setTerm}/>

      <Card className="border">
        <CardContent>
          {term.trim().length === 0 && (
            <EmptyState
              icon={<SearchIcon size="2rem"/>}
              title={<SearchPropertiesPromptTitle/>}
              description={<SearchPropertiesPromptDescription/>}
            />
          )}

          {term.trim().length > 0 && isPending && (
            <div data-slot="property-search-skeleton" className="space-y-3">
              <Skeleton className="h-4 w-64"/>
              <Skeleton className="h-4 w-48"/>
              <Skeleton className="h-4 w-56"/>
            </div>
          )}

          {isError && (
            <ErrorState error={error} className="border-none"/>
          )}

          {term.trim().length > 0 && data?.data.length === 0 && (
            <EmptyState
              icon={<SearchIcon size="2rem"/>}
              title={<NoMatchingPropertiesTitle/>}
            />
          )}

          {data && data.data.length > 0 && (
            <ItemGroup>
              {data.data.map(property => (
                <PropertyItem key={property.name} property={property}/>
              ))}
            </ItemGroup>
          )}
        </CardContent>
      </Card>
    </>
  );
}
