import { z } from 'zod';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useNamespace, useSearchArtifactProperties } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { PropertySearchLabel } from '@konfigyr/components/artifactory/search/messages';
import { PropertyFilters } from '@konfigyr/components/artifactory/search/property-filters';
import { PropertySkeleton } from '@konfigyr/components/artifactory/search/property-item';
import { PropertyTable } from '@konfigyr/components/artifactory/search/property-table';

import type { PropertySearchQuery } from '@konfigyr/hooks/artifactory/types';

const searchQuerySchema = z.object({
  term: z.string().min(2).optional().catch(undefined),
  page: z.number().optional().catch(undefined),
  size: z.number().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/search/',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const query: PropertySearchQuery = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const { data, isPending, error } = useSearchArtifactProperties(namespace.slug, query);

  const onQueryChange = useCallback(async (value: PropertySearchQuery) => {
    await navigate({
      search: current => ({ ...current, ...value, page: 1 }),
      viewTransition: false,
    });
  }, []);

  return (
    <LayoutContent>
      <LayoutNavbar title={( <PropertySearchLabel/> )}/>

      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <p className="text-sm text-muted-foreground max-w-2xl">
          <FormattedMessage
            defaultMessage="Search property names and descriptions across every public artifact on the platform, plus your own private ones, never another namespace's private artifacts."
            description="Description of the artifact property search page."
          />
        </p>

        <div className="flex justify-between items-center gap-4">
          <PropertyFilters
            query={query}
            onQueryChange={onQueryChange}
          />
        </div>

        {isPending && (
          <PropertySkeleton />
        )}

        {error && (
          <ErrorState error={error} className="border-none"/>
        )}

        {data && (
          <PropertyTable
            properties={data}
            page={query.page}
            size={query.size}
          />
        )}
      </div>
    </LayoutContent>
  );
}
