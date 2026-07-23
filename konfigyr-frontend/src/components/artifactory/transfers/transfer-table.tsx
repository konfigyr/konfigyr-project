import { ArrowLeftRightIcon, Package } from 'lucide-react';
import { FormattedDate, FormattedMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import {
  ActionsLabel,
  CreatedAtLabel,
  NamespaceLabel,
  ViewLabel,
} from '@konfigyr/components/messages';
import { ErrorState } from '@konfigyr/components/error';
import { buttonVariants } from '@konfigyr/components/ui/button';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { PageResponsePagination } from '@konfigyr/components/ui/pagination';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@konfigyr/components/ui/table';
import { GroupIdLabel, StateLabel } from '@konfigyr/components/artifactory/transfers/messages';
import { TransferStateBadge } from '@konfigyr/components/artifactory/transfers/transfer-state-badge';

import type { ArtifactOwnershipTransfer, PageResponse } from '@konfigyr/hooks/types';

export type TransferDirection = 'incoming' | 'outgoing';

function TransferRow ({ namespace, direction, transfer }: {
  namespace: string;
  direction: TransferDirection;
  transfer: ArtifactOwnershipTransfer;
}) {
  const counterpart = direction === 'incoming' ? transfer.to : transfer.from;

  return (
    <TableRow>
      <TableCell className="font-mono font-bold">
        <div className="flex items-center gap-2">
          <Package className="size-4 text-muted-foreground" aria-hidden="true"/>
          {transfer.groupId}
        </div>
      </TableCell>
      <TableCell>
        {counterpart.slug}
      </TableCell>
      <TableCell>
        <TransferStateBadge state={transfer.state}/>
      </TableCell>
      <TableCell>
        {transfer.requestedAt ? (
          <time dateTime={transfer.requestedAt}>
            <FormattedDate value={transfer.requestedAt} day="2-digit" month="short" year="numeric"/>
          </time>
        ) : (
          <span className="text-muted-foreground">&mdash;</span>
        )}
      </TableCell>
      <TableCell className="text-right">
        <Link
          to="/namespace/$namespace/artifactory/transfers/$transferId"
          params={{ namespace, transferId: transfer.id }}
          className={buttonVariants({ variant: 'ghost' })}
        >
          <ViewLabel/>
        </Link>
      </TableCell>
    </TableRow>
  );
}

function TransferSkeleton () {
  return (
    <TableRow data-slot="transfer-skeleton">
      <TableCell><Skeleton className="h-5 w-48"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24"/></TableCell>
      <TableCell><Skeleton className="h-5 w-20 rounded-xl"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24 ml-auto"/></TableCell>
    </TableRow>
  );
}

export function TransferTable ({ namespace, direction, data, error, isPending, page, size }: {
  namespace: string;
  direction: TransferDirection;
  data?: PageResponse<ArtifactOwnershipTransfer>;
  error?: Error | null;
  isPending?: boolean;
  page?: number;
  size?: number;
}) {
  return (
    <>
      <Card className="border">
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-64">
                  <GroupIdLabel/>
                </TableHead>
                <TableHead className="w-40">
                  <NamespaceLabel/>
                </TableHead>
                <TableHead className="w-32">
                  <StateLabel/>
                </TableHead>
                <TableHead className="w-32">
                  <CreatedAtLabel/>
                </TableHead>
                <TableHead className="w-48 text-right">
                  <ActionsLabel/>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending && (
                <>
                  <TransferSkeleton/>
                  <TransferSkeleton/>
                  <TransferSkeleton/>
                </>
              )}

              {data?.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <EmptyState
                      icon={<ArrowLeftRightIcon size="2rem"/>}
                      title={
                        <FormattedMessage
                          defaultMessage="No ownership transfers found"
                          description="Empty state title when no ownership transfers are found."
                        />
                      }
                      description={
                        direction === 'incoming' ? (
                          <FormattedMessage
                            defaultMessage="No other namespace has asked this namespace to transfer ownership of a groupId."
                            description="Empty state description when no incoming ownership transfers are found."
                          />
                        ) : (
                          <FormattedMessage
                            defaultMessage="This namespace has not requested ownership of a groupId from another namespace."
                            description="Empty state description when no outgoing ownership transfers are found."
                          />
                        )
                      }
                    />
                  </TableCell>
                </TableRow>
              )}

              {data?.data.map((transfer) => (
                <TransferRow key={transfer.id} namespace={namespace} direction={direction} transfer={transfer}/>
              ))}
            </TableBody>
          </Table>

          {error && <ErrorState error={error}/>}
        </CardContent>
      </Card>

      <PageResponsePagination page={page} size={size} response={data} className="mt-4"/>
    </>
  );
}
