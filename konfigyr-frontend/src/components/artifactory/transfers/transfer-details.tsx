import {
  ArrowLeftRightIcon,
  CheckIcon,
  XIcon,
} from 'lucide-react';
import { FormattedDate } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import { TransferStateBadge } from '@konfigyr/components/artifactory/transfers/transfer-state-badge';
import { NamespaceLabel } from '@konfigyr/components/messages';
import {
  AcceptTransferLabel,
  CancelTransferLabel,
  RejectTransferLabel,
  RequestedAtLabel,
  ResolvedAtLabel,
} from '@konfigyr/components/artifactory/transfers/messages';
import {
  AcceptTransferButton,
  CancelTransferButton,
  RejectTransferButton,
} from '@konfigyr/components/artifactory/transfers/transfer-actions';
import { TransferAuditTrail } from '@konfigyr/components/artifactory/transfers/transfer-audit-trail';

import type { ArtifactOwnershipTransfer, Namespace } from '@konfigyr/hooks/types';

export function TransferDetails({ namespace, transfer }: {
  namespace: Namespace;
  transfer: ArtifactOwnershipTransfer;
}) {
  const isFromParty = transfer.from.slug === namespace.slug;
  const isToParty = transfer.to.slug === namespace.slug;

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border bg-card">
            <ArrowLeftRightIcon className="size-5 text-muted-foreground" aria-hidden="true"/>
          </div>
          <div className="min-w-0">
            <h1 className="truncate font-mono text-2xl font-medium leading-tight">
              {transfer.groupId}
            </h1>
          </div>
        </div>
        <TransferStateBadge state={transfer.state}/>
      </div>

      <dl className="grid grid-cols-3 gap-4 text-sm">
        <div>
          <dt className="text-muted-foreground"><NamespaceLabel/></dt>
          <dd>{isFromParty ? transfer.to.slug : transfer.from.slug}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground"><RequestedAtLabel/></dt>
          <dd>
            {transfer.requestedAt ? (
              <time dateTime={transfer.requestedAt}>
                <FormattedDate value={transfer.requestedAt} day="2-digit" month="short" year="numeric"/>
              </time>
            ) : (
              <span className="text-muted-foreground">&mdash;</span>
            )}
          </dd>
        </div>
        <div>
          <dt className="text-muted-foreground"><ResolvedAtLabel/></dt>
          <dd>
            {transfer.resolvedAt ? (
              <time dateTime={transfer.resolvedAt}>
                <FormattedDate value={transfer.resolvedAt} day="2-digit" month="short" year="numeric"/>
              </time>
            ) : (
              <span className="text-muted-foreground">&mdash;</span>
            )}
          </dd>
        </div>
      </dl>

      <div className="flex flex-wrap gap-3">
        {transfer.state === 'PENDING' && isFromParty && (
          <>
            <AcceptTransferButton namespace={namespace.slug} transfer={transfer}>
              <Button>
                <CheckIcon/>
                <AcceptTransferLabel/>
              </Button>
            </AcceptTransferButton>

            <RejectTransferButton namespace={namespace.slug} transfer={transfer}>
              <Button variant="destructive">
                <XIcon/>
                <RejectTransferLabel/>
              </Button>
            </RejectTransferButton>
          </>
        )}

        {transfer.state === 'PENDING' && isToParty && (
          <CancelTransferButton namespace={namespace.slug} transfer={transfer}>
            <Button variant="destructive">
              <XIcon/>
              <CancelTransferLabel/>
            </Button>
          </CancelTransferButton>
        )}
      </div>

      <TransferAuditTrail
        namespace={namespace}
        transferId={transfer.id}
      />
    </>
  );
}
