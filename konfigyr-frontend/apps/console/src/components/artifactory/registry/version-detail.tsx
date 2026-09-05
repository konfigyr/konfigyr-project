import { FormattedDate, FormattedMessage, useIntl } from 'react-intl';
import { TagIcon } from 'lucide-react';
import { useSearchArtifactProperties } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { ArtifactVisibilityBadge } from '@konfigyr/components/artifactory/registry/visibility-badge';
import { PropertyTable } from '@konfigyr/components/artifactory/search/property-table';
import { PropertySearchField } from '@konfigyr/components/artifactory/search/property-search-field';
import { PublishedAtLabel } from '@konfigyr/components/artifactory/registry/messages';

import type { VersionedArtifact } from '@konfigyr/hooks/artifactory/types';

function PropertySearchForm({ term, onTermChange }: { term: string; onTermChange: (term: string) => void }) {
  const intl = useIntl();

  return (
    <PropertySearchField
      term={term}
      label={intl.formatMessage({
        defaultMessage: 'Search properties in this version',
        description: 'Aria label for the version property search input.',
      })}
      onTermChange={onTermChange}
    />
  );
}

export function VersionDetail({ namespace, version, term, page, size, onTermChange }: {
  namespace: string;
  version: VersionedArtifact;
  term?: string;
  page?: number;
  size?: number;
  onTermChange: (term: string) => void;
}) {
  const { data, error, isPending, isError } = useSearchArtifactProperties(namespace, {
    groupId: version.groupId,
    artifactId: version.artifactId,
    version: version.version,
    page,
    size,
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

      <PropertySearchForm term={term ?? ''} onTermChange={onTermChange}/>

      {isPending && (
        <div data-slot="property-search-skeleton" className="border border-accent rounded-xl p-4">
          <Skeleton className="h-4 w-72 mb-2"/>
          <Skeleton className="h-4 w-48 mb-3"/>
          <Skeleton className="h-4 w-56 mb-1"/>
          <Skeleton className="h-4 w-64"/>
        </div>
      )}

      {isError && (
        <ErrorState error={error} />
      )}

      {data && (
        <div>
          <div className="flex justify-between mb-2">
            <p className="font-heading font-semibold test-lg">Property definitions</p>
            <p className="text-sm text-muted-foreground">
              <FormattedMessage
                defaultMessage="{count, plural, one {1 property} other {# properties}}"
                description="Label used to describe the number of properties in a version."
                values={{ count: data.metadata.total }}
              />
            </p>
          </div>
          <PropertyTable page={page} size={size} properties={data} variant="version" />
        </div>
      )}
    </>
  );
}
