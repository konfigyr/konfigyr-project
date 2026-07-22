import { z } from 'zod';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router';
import { useGetIncomingTransfers, useGetOutgoingTransfers, useNamespace } from '@konfigyr/hooks';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { buttonVariants } from '@konfigyr/components/ui/button';
import { TransferFilters } from '@konfigyr/components/artifactory/transfers/transfer-filters';
import { TransferTable } from '@konfigyr/components/artifactory/transfers/transfer-table';
import { IncomingLabel, OutgoingLabel, RequestTransferLabel, TransfersLabel } from '@konfigyr/components/artifactory/transfers/messages';

import type { ArtifactOwnershipTransferQuery } from '@konfigyr/hooks/types';

const searchQuerySchema = z.object({
  term: z.string().min(2).optional().catch(undefined),
  page: z.number().optional().catch(undefined),
  size: z.number().optional().catch(undefined),
  direction: z.enum(['incoming', 'outgoing']).optional().catch('incoming'),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/transfers/',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function DirectionToggle ({ direction }: { direction: 'incoming' | 'outgoing' }) {
  return (
    <div className="flex gap-1">
      <Link
        to="."
        search={search => ({ ...search, direction: 'incoming', page: 1 })}
        className={buttonVariants({ variant: direction === 'incoming' ? 'secondary' : 'ghost' })}
      >
        <IncomingLabel/>
      </Link>
      <Link
        to="."
        search={search => ({ ...search, direction: 'outgoing', page: 1 })}
        className={buttonVariants({ variant: direction === 'outgoing' ? 'secondary' : 'ghost' })}
      >
        <OutgoingLabel/>
      </Link>
    </div>
  );
}

function RouteComponent() {
  const namespace = useNamespace();
  const search = Route.useSearch();
  const query: ArtifactOwnershipTransferQuery = search;
  const direction = search.direction ?? 'incoming';
  const navigate = useNavigate({ from: Route.fullPath });

  const incoming = useGetIncomingTransfers(namespace.slug, query);
  const outgoing = useGetOutgoingTransfers(namespace.slug, query);
  const { data, isPending, error } = direction === 'incoming' ? incoming : outgoing;

  const onQueryChange = useCallback(async (value: ArtifactOwnershipTransferQuery) => {
    await navigate({
      search: current => ({ ...current, ...value, page: 1 }),
      viewTransition: false,
    });
  }, []);

  return (
    <LayoutContent>
      <LayoutNavbar title={( <TransfersLabel/> )}/>
      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <p className="text-sm text-muted-foreground max-w-2xl">
          <FormattedMessage
            defaultMessage="Request, accept, reject, or cancel ownership transfers of Maven groupId coordinates between namespaces."
            description="Description of the ownership transfers page."
          />
        </p>

        <DirectionToggle direction={direction}/>

        <div className="flex justify-between items-center gap-4">
          <TransferFilters
            query={query}
            onQueryChange={onQueryChange}
          />

          <Link
            to="/namespace/$namespace/artifactory/transfers/create"
            params={{ namespace: namespace.slug }}
            className={buttonVariants({ variant: 'default' })}
          >
            <RequestTransferLabel/>
          </Link>
        </div>

        <TransferTable
          namespace={namespace.slug}
          direction={direction}
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
