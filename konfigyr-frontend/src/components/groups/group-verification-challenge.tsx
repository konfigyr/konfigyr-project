import { FormattedMessage, useIntl } from 'react-intl';
import { GroupVerificationState } from '@konfigyr/components/groups/group-verification-state';
import { VerificationMethodName } from '@konfigyr/components/groups/group-verification-method';
import { Card, CardContent, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import {
  ChallengeStateLabel,
  GroupIdLabel,
  MethodLabel,
  RevokedAtLabel,
  StateLabel,
  VerifiedAtLabel,
} from '@konfigyr/components/groups/messages';
import { CreatedAtLabel } from '@konfigyr/components/messages';
import {
  DnsActivationInstruction,
  SourceCodeActivationInstruction,
} from '@konfigyr/components/groups/group-verification-instruction';
import type { ChallengeState, GroupVerification, VerificationChallenge } from '@konfigyr/hooks/types';
import type { ReactNode } from 'react';

const DATE_FORMAT_OPTIONS: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
};

export function GroupVerification ({ verification, challenge }: {
  verification: GroupVerification;
  challenge?: VerificationChallenge | undefined;
}) {
  const isChallengeVisible = Boolean(challenge && verification.state !== 'REVOKED');

  let instructions: React.ReactNode = null;

  if (challenge) {
    instructions =
      challenge.method === 'DNS' ? (
        <DnsActivationInstruction groupId={verification.groupId} challenge={challenge}/>
      ) : (
        <SourceCodeActivationInstruction groupId={verification.groupId} challenge={challenge}/>
      );
  }

  return (
    <>
      <GroupVerificationDetails verification={verification} challenge={challenge}/>
      { isChallengeVisible && verification.state === 'PENDING' && instructions }
    </>
  );
}

export function GroupVerificationChallengeState ({ state }: { state?: string | ChallengeState }) {
  switch (state) {
    case 'UNVERIFIED':
      return <FormattedMessage
        defaultMessage="Unverified"
        description="Name or label of the UNVERIFIED group verification challenge state."
      />;
    case 'VERIFIED':
      return <FormattedMessage
        defaultMessage="Verified"
        description="Name or label of the VERIFIED group verification challenge state."
      />;
    case 'EXPIRED':
      return <FormattedMessage
        defaultMessage="Expired"
        description="Name or label of the EXPIRED group verification challenge state."
      />;
    default:
      return null;
  }
}

export function GroupVerificationDetails ({ verification, challenge }: { verification: GroupVerification, challenge?: VerificationChallenge | null }) {
  const intl = useIntl();
  const createdAt = verification.createdAt ? intl.formatDate(verification.createdAt, DATE_FORMAT_OPTIONS) : null;
  const verifiedAt = verification.verifiedAt ? intl.formatDate(verification.verifiedAt, DATE_FORMAT_OPTIONS) : null;
  const revokedAt = verification.revokedAt ? intl.formatDate(verification.revokedAt, DATE_FORMAT_OPTIONS) : null;
  const method = challenge ? <VerificationMethodName method={challenge.method}/> : null;
  const challengeState = challenge ? <GroupVerificationChallengeState state={challenge.state}/> : null;
  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="text-base text-muted-foreground">
          <FormattedMessage
            defaultMessage="Group verification details"
            description="Label for the group verification detail card."
          />
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <dl className="grid gap-6 md:grid-cols-2">
          <DetailItem label={(<GroupIdLabel/>)} value={verification.groupId}/>
          <DetailItem label={(<StateLabel/>)} value={(<GroupVerificationState state={verification.state}/>)}/>
          <DetailItem label={(<MethodLabel/>)} value={method}/>
          { challengeState && <DetailItem label={(<ChallengeStateLabel/>)} value={challengeState}/> }
          <DetailItem label={(<CreatedAtLabel/>)} value={createdAt}/>
          { verifiedAt && <DetailItem label={(<VerifiedAtLabel/>)} value={verifiedAt}/> }
          { revokedAt && <DetailItem label={(<RevokedAtLabel/>)} value={revokedAt}/> }
        </dl>
      </CardContent>
    </Card>
  );
}

function DetailItem ({ label, value }: { label: ReactNode; value?: ReactNode }) {
  return (
    <div className="grid gap-1">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value ? value : <>&mdash;</>}</dd>
    </div>
  );
}
