import { useMemo } from 'react';
import {
  PackageIcon,
  RotateCcwIcon,
  ShieldBanIcon,
  XIcon,
} from 'lucide-react';
import { Link } from '@tanstack/react-router';
import { Button, buttonVariants } from '@konfigyr/components/ui/button';
import { RequestTransferLabel } from '@konfigyr/components/artifactory/transfers/messages';
import {
  ConflictingOwnersAlert,
  GroupVerificationStateAlert,
} from '@konfigyr/components/artifactory/groups/group-verification-state';
import { GroupVerificationChallenge } from '@konfigyr/components/artifactory/groups/group-verification-challenge';
import {
  CancelGroupVerificationButton,
  RevokeGroupVerificationButton,
} from '@konfigyr/components/artifactory/groups/revoke-group-verification';
import {
  CancelClaimLabel,
  ClaimAgainLabel,
  RevokeClaimLabel,
  VerifyClaimLabel,
} from '@konfigyr/components/artifactory/groups/messages';
import { VerifyGroupVerificationButton } from '@konfigyr/components/artifactory/groups/verify-group-verification';

import type { GroupVerification, Namespace, VerificationChallenge } from '@konfigyr/hooks/types';

function RequestTransferCta({ namespace, groupId, conflictingOwners }: {
  namespace: string;
  groupId: string;
  conflictingOwners: Array<string>;
}) {
  const fromNamespace = conflictingOwners.length === 1 ? conflictingOwners[0] : undefined;

  return (
    <Link
      to="/namespace/$namespace/artifactory/transfers/create"
      params={{ namespace }}
      search={{ groupId, fromNamespace }}
      className={buttonVariants({ variant: 'outline', size: 'sm' })}
    >
      <RequestTransferLabel/>
    </Link>
  );
}

export function GroupVerificationDetails({ namespace, verification, challenges }: {
  namespace: Namespace;
  verification: GroupVerification;
  challenges: Array<VerificationChallenge>;
}) {
  const challenge = useMemo(
    () => challenges.at(-1),
    [challenges],
  );

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border bg-card">
            <PackageIcon className="size-5 text-muted-foreground" aria-hidden="true"/>
          </div>
          <div className="min-w-0">
            <h1 className="truncate font-mono text-2xl font-medium leading-tight">
              {verification.groupId}
            </h1>
          </div>
        </div>
      </div>

      <GroupVerificationStateAlert
        groupId={verification.groupId}
        verificationChallenge={challenge}
        verificationState={verification.state}
      />

      {!!verification.conflictingOwners?.length && (
        <ConflictingOwnersAlert
          conflictingOwners={verification.conflictingOwners}
          action={(
            <RequestTransferCta
              namespace={namespace.slug}
              groupId={verification.groupId}
              conflictingOwners={verification.conflictingOwners}
            />
          )}
        />
      )}

      <GroupVerificationChallenge verification={verification} challenge={challenge}/>

      <div className="flex flex-wrap gap-3">
        {verification.state === 'ACTIVE' && (
          <RevokeGroupVerificationButton namespace={namespace.slug} verification={verification} >
            <Button variant="destructive">
              <ShieldBanIcon />
              <RevokeClaimLabel/>
            </Button>
          </RevokeGroupVerificationButton>
        )}

        {verification.state === 'PENDING' && (
          <CancelGroupVerificationButton namespace={namespace.slug} verification={verification} >
            <Button variant="destructive">
              <XIcon/>
              <CancelClaimLabel />
            </Button>
          </CancelGroupVerificationButton>
        )}

        {(verification.state === 'PENDING' || verification.state === 'FAILED') && (
          <VerifyGroupVerificationButton namespace={namespace.slug} verification={verification}>
            <Button>
              <RotateCcwIcon/>
              <VerifyClaimLabel/>
            </Button>
          </VerifyGroupVerificationButton>
        )}

        {verification.state === 'REVOKED' && (
          <Link
            to="/namespace/$namespace/artifactory/groups/$groupId/edit"
            params={{ namespace: namespace.slug, groupId: verification.groupId }}
            className={buttonVariants({ variant: 'default' })}
          >
            <RotateCcwIcon/>
            <ClaimAgainLabel/>
          </Link>
        )}
      </div>
    </>
  );
}
