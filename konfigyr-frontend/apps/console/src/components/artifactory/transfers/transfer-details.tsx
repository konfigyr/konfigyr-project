import {
  ArrowLeftRightIcon,
  ArrowRightIcon,
  CheckIcon,
  XIcon,
} from 'lucide-react';
import { FormattedDate } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from '@konfigyr/components/ui/card';
import { TransferStateBadge } from '@konfigyr/components/artifactory/transfers/transfer-state-badge';
import { NamespaceLabel } from '@konfigyr/components/messages';
import {
  AcceptTransferLabel,
  CancelTransferLabel,
  CurrentOwnerLabel,
  RejectTransferLabel,
  RequestedAtLabel,
  RequestingNamespaceLabel,
  ResolvedAtLabel,
  StateLabel,
  TransferDetailsLabel,
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

      <Card className="border">
        <CardContent className="flex items-center justify-center gap-4 py-2">
          <div className="grow text-center">
            <p className="text-xs font-semibold  uppercase text-muted-foreground">
              <CurrentOwnerLabel/>
            </p>
            <p className="font-heading font-semibold leading-relaxed">
              {transfer.from.slug}
            </p>
          </div>
          <ArrowRightIcon className="size-6" />
          <div className="grow text-center">
            <p className="text-xs font-semibold uppercase text-muted-foreground">
              <RequestingNamespaceLabel/>
            </p>
            <p className="font-heading font-semibold leading-relaxed">
              {transfer.to.slug}
            </p>
          </div>
        </CardContent>
      </Card>

      <Card className="border">
        <CardHeader title={<TransferDetailsLabel />} />
        <CardContent>
          <dl className="grid grid-cols-2 gap-4 text-sm">
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
              <dt className="text-muted-foreground"><StateLabel/></dt>
              <dd><TransferStateBadge state={transfer.state}/></dd>
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
        </CardContent>

        {transfer.state === 'PENDING' && isFromParty && (
          <CardFooter className="gap-3">
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
          </CardFooter>
        )}

        {transfer.state === 'PENDING' && isToParty && (
          <CardFooter>
            <CancelTransferButton namespace={namespace.slug} transfer={transfer}>
              <Button variant="destructive">
                <XIcon/>
                <CancelTransferLabel/>
              </Button>
            </CancelTransferButton>
          </CardFooter>
        )}
      </Card>

      <TransferAuditTrail
        namespace={namespace}
        transferId={transfer.id}
      />
    </>
  );
}
