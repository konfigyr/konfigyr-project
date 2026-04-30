import { FormattedDate, FormattedMessage } from 'react-intl';
import { Link2OffIcon } from 'lucide-react';
import { Link } from '@tanstack/react-router';
import { useGetNamespaceInvitations } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { NamespaceRoleBadge } from '@konfigyr/components/namespace/role';
import { Card, CardContent } from '@konfigyr/components/ui/card';
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@konfigyr/components/ui/table';

import type { Invitation, Namespace, PageResponse, Pageable } from '@konfigyr/hooks/types';

function InvitationPagination({ page = 1, size = 20, data }: {
  page?: number;
  size?: number;
  data?: PageResponse<Invitation>;
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
            render={(
              <Link to="." search={search => ({ ...search, page: page - 1 })} />
            )}
          />
        </PaginationItem>
        <PaginationRange>
          {(state) => (
            <PaginationLink
              isActive={state.active}
              render={(
                <Link to="." search={search => ({ ...search, page: state.page })}>
                  {state.page}
                </Link>
              )}
            />
          )}
        </PaginationRange>
        <PaginationItem>
          <PaginationNext
            render={(
              <Link to="." search={search => ({ ...search, page: page + 1 })} />
            )}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}

export function Invitations({ namespace, pageable }: { namespace: Namespace, pageable?: Pageable }) {
  const { data: invitations, error, isError, isPending } = useGetNamespaceInvitations(namespace, pageable);

  return (
    <>
      <Card className="border">
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  <FormattedMessage
                    defaultMessage="Recipient"
                    description="Label for the recipient column in the invitations table."
                  />
                </TableHead>
                <TableHead>
                  <FormattedMessage
                    defaultMessage="Role"
                    description="Label for the role column in the invitations table."
                  />
                </TableHead>
                <TableHead>
                  <FormattedMessage
                    defaultMessage="Sender"
                    description="Label for the sender column in the invitations table."
                  />
                </TableHead>
                <TableHead>
                  <FormattedMessage
                    defaultMessage="Created at"
                    description="Label for the created at column in the invitations table."
                  />
                </TableHead>
                <TableHead>
                  <FormattedMessage
                    defaultMessage="Expires at"
                    description="Label for the expires at column in the invitations table."
                  />
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending && (
                <TableRow data-slot="invitations-skeleton">
                  <TableCell>
                    <Skeleton className="h-5 w-36 mb-2" />
                    <Skeleton className="h-5 w-48" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-5 w-12 rounded-xl" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-5 w-36 mb-2" />
                    <Skeleton className="h-5 w-48" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-5 w-24" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-5 w-24" />
                  </TableCell>
                </TableRow>
              )}

              {invitations?.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <EmptyState
                      title="No invitations found"
                      description="There are currently no pending invitations for this namespace."
                      icon={<Link2OffIcon size="2rem" />}
                    />
                  </TableCell>
                </TableRow>
              )}

              {invitations?.data.map(invitation => (
                <TableRow key={invitation.key}>
                  <TableCell>
                    {invitation.recipient.name && (
                      <p className="font-medium">{invitation.recipient.name}</p>
                    )}
                    <p className="text-sm text-muted-foreground font-mono">{invitation.recipient.email}</p>
                  </TableCell>
                  <TableCell>
                    <NamespaceRoleBadge role={invitation.role} variant="outline" />
                  </TableCell>
                  <TableCell>
                    <p className="font-medium">{invitation.sender.name}</p>
                    <p className="text-sm text-muted-foreground font-mono">{invitation.sender.email}</p>
                  </TableCell>
                  <TableCell>
                    <FormattedDate value={invitation.createdAt} />
                  </TableCell>
                  <TableCell>
                    <FormattedDate value={invitation.expiryDate} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {isError && (
            <ErrorState error={error} />
          )}
        </CardContent>
      </Card>

      <InvitationPagination
        page={pageable?.page}
        size={pageable?.size}
        data={invitations}
      />
    </>
  );
}
