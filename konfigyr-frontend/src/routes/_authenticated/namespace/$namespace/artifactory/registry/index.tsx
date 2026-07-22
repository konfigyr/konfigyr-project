import { z } from 'zod';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useGetArtifacts, useNamespace } from '@konfigyr/hooks';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { ArtifactFilters } from '@konfigyr/components/artifactory/registry/artifact-filters';
import { ArtifactTable } from '@konfigyr/components/artifactory/registry/artifact-table';
import { RegistryLabel } from '@konfigyr/components/artifactory/registry/messages';

import type { ArtifactQuery } from '@konfigyr/hooks/artifactory/types';

const searchQuerySchema = z.object({
  term: z.string().min(2).optional().catch(undefined),
  page: z.number().optional().catch(undefined),
  size: z.number().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/registry/',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const query: ArtifactQuery = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const { data, isPending, error } = useGetArtifacts(namespace.slug, query);

  const onQueryChange = useCallback(async (value: ArtifactQuery) => {
    await navigate({
      search: current => ({ ...current, ...value, page: 1 }),
      viewTransition: false,
    });
  }, []);

  return (
    <LayoutContent>
      <LayoutNavbar title={( <RegistryLabel/> )}/>
      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <p className="text-sm text-muted-foreground max-w-2xl">
          <FormattedMessage
            defaultMessage="Browse every artifact this namespace has published, public and private, and drill into a version to inspect its configuration property definitions."
            description="Description of the artifact registry page."
          />
        </p>

        <div className="flex justify-between items-center gap-4">
          <ArtifactFilters
            query={query}
            onQueryChange={onQueryChange}
          />
        </div>

        <ArtifactTable
          namespace={namespace.slug}
          data={data}
          isPending={isPending}
          error={error}
          page={query.page}
          size={query.size}
        />
      </div>
    </LayoutContent>
  );
}
