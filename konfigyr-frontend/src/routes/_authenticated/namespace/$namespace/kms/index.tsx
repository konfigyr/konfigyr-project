import { z } from 'zod';
import { useCallback } from 'react';
import { PlusIcon } from 'lucide-react';
import { useGetKeysets, useNamespace } from '@konfigyr/hooks';
import { CreateKeysetLabel } from '@konfigyr/components/kms/messages';
import { KeysetFilters } from '@konfigyr/components/kms/keyset-filters';
import { KeysetTable } from '@konfigyr/components/kms/keyset-table';
import { Button } from '@konfigyr/components/ui/button';
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router';

import type { KeysetSearchQuery } from '@konfigyr/hooks/types';

const searchQuerySchema = z.object({
  term: z.string().min(3).optional().catch(undefined),
  state: z.enum(['ACTIVE', 'INACTIVE', 'PENDING_DESTRUCTION', 'DESTROYED']).optional().catch(undefined),
  algorithm: z.string().optional().catch(undefined),
  sort: z.string().optional().catch(undefined),
  page: z.number().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/kms/',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const query: KeysetSearchQuery = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const { data: keysets, isPending, error } = useGetKeysets(namespace.slug, query);

  const onQueryChange = useCallback(async (value: KeysetSearchQuery) => {
    await navigate({
      search: current => ({ ...current, ...value }),
      viewTransition: false,
    });
  }, []);

  return (
    <div className="w-full space-y-6 px-4">
      <div className="flex justify-between items-center gap-4">
        <KeysetFilters
          query={query}
          onQueryChange={onQueryChange}
        />

        <Button variant="ghost" asChild>
          <Link
            to="/namespace/$namespace/kms/create"
            params={{ namespace: namespace.slug }}
          >
            <PlusIcon size="1rem"/>
            <CreateKeysetLabel />
          </Link>
        </Button>
      </div>

      <KeysetTable
        namespace={namespace}
        keysets={keysets}
        error={error}
        isPending={isPending}
      />
    </div>
  );
}
