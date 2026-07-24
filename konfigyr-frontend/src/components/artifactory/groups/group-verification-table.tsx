import { FormattedDate, FormattedMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { ActionsLabel, CreatedAtLabel, ViewLabel } from '@konfigyr/components/messages';
import { ErrorState } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { PageResponsePagination } from '@konfigyr/components/ui/pagination';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@konfigyr/components/ui/table';
import { EllipsisVerticalIcon, Package, ShieldCheckIcon } from 'lucide-react';
import {
  CancelClaimLabel,
  GroupIdLabel,
  RevokeClaimLabel,
  StateLabel,
  VerifiedAtLabel,
} from '@konfigyr/components/artifactory/groups/messages';
import { useState } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { GroupVerificationStateBadge } from './group-verification-state';
import { CancelGroupVerificationButton, RevokeGroupVerificationButton } from './revoke-group-verification';
import type { ReactNode } from 'react';

import type { GroupVerification, PageResponse } from '@konfigyr/hooks/types';

export function GroupVerificationDetailsLink ({ namespace, groupId, children }: {
  namespace: string,
  groupId: string,
  children?: ReactNode
}) {
  return (
    <Link
      to="/namespace/$namespace/artifactory/groups/$groupId"
      params={{ namespace, groupId }}
    >
      {children ? children : groupId}
    </Link>
  );
}

function GroupVerificationRowActions ({ namespace, verification }: {
  namespace: string;
  verification: GroupVerification;
}) {
  const [menuOpen, setMenuOpen] = useState(false);
  const handleDialogOpenChange = (open: boolean) => {
    if (!open) {
      setMenuOpen(false);
    }
  };

  return (
    <DropdownMenu open={menuOpen} onOpenChange={setMenuOpen}>
      <DropdownMenuTrigger
        render={
          <Button variant="ghost">
            <EllipsisVerticalIcon/>
          </Button>
        }
      />
      <DropdownMenuContent align="end">
        <GroupVerificationDetailsLink namespace={namespace} groupId={verification.groupId}>
          <DropdownMenuItem>
            <ViewLabel/>
          </DropdownMenuItem>
        </GroupVerificationDetailsLink>

        {verification.state === 'ACTIVE' && (
          <RevokeGroupVerificationButton
            nativeButton={false}
            role="menuitem"
            namespace={namespace}
            verification={verification}
            onOpenChange={handleDialogOpenChange}
          >
            <DropdownMenuItem closeOnClick={false}>
              <RevokeClaimLabel/>
            </DropdownMenuItem>
          </RevokeGroupVerificationButton>
        )}

        {verification.state === 'PENDING' && (
          <CancelGroupVerificationButton
            nativeButton={false}
            role="menuitem"
            namespace={namespace}
            verification={verification}
            onOpenChange={handleDialogOpenChange}
          >
            <DropdownMenuItem closeOnClick={false}>
              <CancelClaimLabel/>
            </DropdownMenuItem>
          </CancelGroupVerificationButton>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function GroupVerificationRow ({ namespace, verification }: {
  namespace: string;
  verification: GroupVerification;
}) {
  return (
    <TableRow>
      <TableCell className="font-mono font-bold">
        <div className="flex items-center gap-2">
          <Package className="size-4 text-muted-foreground" aria-hidden="true"/>
          {verification.groupId}
        </div>
      </TableCell>
      <TableCell>
        <GroupVerificationStateBadge state={verification.state}/>
      </TableCell>
      <TableCell>
        <time dateTime={verification.createdAt}>
          <FormattedDate value={verification.createdAt} day="2-digit" month="short" year="numeric"/>
        </time>
      </TableCell>
      <TableCell>
        {verification.verifiedAt ? (
          <time dateTime={verification.verifiedAt}>
            <FormattedDate value={verification.verifiedAt} day="2-digit" month="short" year="numeric"/>
          </time>
        ) : (
          <span className="text-muted-foreground">&mdash;</span>
        )}
      </TableCell>
      <TableCell className="text-right">
        <GroupVerificationRowActions namespace={namespace} verification={verification}/>
      </TableCell>
    </TableRow>
  );
}

function GroupVerificationSkeleton () {
  return (
    <TableRow data-slot="group-verification-skeleton">
      <TableCell><Skeleton className="h-5 w-48"/></TableCell>
      <TableCell><Skeleton className="h-5 w-20 rounded-xl"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24 ml-auto"/></TableCell>
    </TableRow>
  );
}

export function GroupVerificationTable ({ namespace, data, error, isPending, page, size }: {
  namespace: string;
  data?: PageResponse<GroupVerification>;
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
                <TableHead className="w-32">
                  <StateLabel/>
                </TableHead>
                <TableHead className="w-32">
                  <CreatedAtLabel/>
                </TableHead>
                <TableHead className="w-32">
                  <VerifiedAtLabel/>
                </TableHead>
                <TableHead className="w-48 text-right">
                  <ActionsLabel/>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending && (
                <>
                  <GroupVerificationSkeleton/>
                  <GroupVerificationSkeleton/>
                  <GroupVerificationSkeleton/>
                </>
              )}

              {data?.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <EmptyState
                      icon={<ShieldCheckIcon size="2rem"/>}
                      title={
                        <FormattedMessage
                          defaultMessage="No group claims found"
                          description="Empty state title when no group verification claims are found."
                        />
                      }
                      description={
                        <FormattedMessage
                          defaultMessage="This namespace has not claimed any Maven groupId coordinates yet."
                          description="Empty state description when no group verification claims are found."
                        />
                      }
                    />
                  </TableCell>
                </TableRow>
              )}

              {data?.data.map((verification) => (
                <GroupVerificationRow key={verification.id} namespace={namespace} verification={verification}/>
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
