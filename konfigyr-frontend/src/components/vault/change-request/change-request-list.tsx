import { FormattedMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { useGetChangeRequests } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { RelativeDate } from '@konfigyr/components/messages';
import { ChangesCountLabel } from '@konfigyr/components/vault/messages';
import { Badge } from '@konfigyr/components/ui/badge';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationRange,
} from '@konfigyr/components/ui/pagination';
import { ChangeRequestFilters } from './change-request-filters';
import { ChangeRequestStateIcon } from './change-request-state';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { ChangeRequest, ChangeRequestQuery } from '@konfigyr/hooks/vault/types';

function ChangeRequestItem({ namespace, service, request }: {
  namespace: Namespace;
  service: Service;
  request: ChangeRequest;
}) {

  return (
    <Item
      size="xs"
      variant="list"
      aria-label={request.subject}
    >
      <ItemMedia variant="icon">
        <ChangeRequestStateIcon value={request.state} />
      </ItemMedia>
      <ItemContent>
        <ItemTitle>
          <Link
            to="/namespace/$namespace/services/$service/requests/$number"
            params={{
              namespace: namespace.slug,
              service: service.slug,
              number: String(request.number),
            }}
            className="hover:text-primary"
          >
            {request.subject}
          </Link>
        </ItemTitle>
        <ItemDescription>
          #{request.number} opened <RelativeDate value={request.createdAt} /> by {request.createdBy}
        </ItemDescription>
      </ItemContent>
      <ItemActions>
        <Badge variant="outline">
          <ChangesCountLabel count={request.count} />
        </Badge>
      </ItemActions>
    </Item>
  );
}

function ChangeRequestItemGroup({ namespace, service, data, error, isPending = false }: {
  namespace: Namespace;
  service: Service;
  data?: PageResponse<ChangeRequest>;
  error?: Error | null;
  isPending?: boolean;
}) {
  if (isPending) {
    return (
      <p data-slot="change-request-list-skeleton">
        Loading
      </p>
    );
  }

  if (error) {
    return (
      <ErrorState error={error} />
    );
  }

  if (data?.data.length === 0) {
    return (
      <EmptyState
        title={
          <FormattedMessage
            defaultMessage="There are no change requests yet."
            description="Empty state title used when no change requests are found."
          />
        }
        description={
          <FormattedMessage
            defaultMessage="You can create a change request by editing a profile configuration state."
            description="Empty state description used when no change requests are found. Should tell the user how to create a change request."
          />
        }
        size="lg"
      />
    );
  }

  return (
    <ItemGroup size="xs" className="border rounded-lg px-3 py-1">
      {data?.data.map(request => (
        <ChangeRequestItem
          key={request.id}
          namespace={namespace}
          service={service}
          request={request}
        />
      ))}
    </ItemGroup>
  );
}

function ChangeRequestPagination({ page = 1, size = 20, data }: {
  page?: number;
  size?: number;
  data?: PageResponse<ChangeRequest>;
}) {
  const pages = data?.metadata.pages || 1;
  const total = data?.metadata.total || 0;

  if (pages < 2) {
    return null;
  }

  return (
    <Pagination page={page} pages={pages} total={total} size={size} className="mt-4">
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            render={
              <Link to="." search={search => ({ ...search, page: page - 1 })} />
            }
          />
        </PaginationItem>
        <PaginationRange>
          {state => (
            <PaginationLink
              isActive={state.active}
              render={
                <Link to="." search={search => ({ ...search, page: state.page })}>
                  {state.page}
                </Link>
              }
            />
          )}
        </PaginationRange>
        <PaginationItem>
          <PaginationNext
            render={
              <Link to="." search={search => ({ ...search, page: page + 1 })} />
            }
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}

export function ChangeRequestList({ namespace, service, query, onQueryChange }: {
  namespace: Namespace;
  service: Service;
  query: ChangeRequestQuery;
  onQueryChange: (query: ChangeRequestQuery) => void;
}) {
  const { data, error, isPending } = useGetChangeRequests(namespace, service, query);

  return (
    <>
      <ChangeRequestFilters
        namespace={namespace}
        service={service}
        query={query}
        onQueryChange={onQueryChange}
      />

      <ChangeRequestItemGroup
        namespace={namespace}
        service={service}
        data={data}
        error={error}
        isPending={isPending}
      />

      <ChangeRequestPagination
        page={query.page}
        size={query.size}
        data={data}
      />
    </>
  );
}
