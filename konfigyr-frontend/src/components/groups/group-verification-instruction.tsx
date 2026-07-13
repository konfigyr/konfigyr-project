import { FormattedMessage } from 'react-intl';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import { ClipboardButton } from '@konfigyr/components/clipboard';
import { sourceCodeHostLabel } from '@konfigyr/components/groups/group-verification-method';
import type { VerificationChallenge } from '@konfigyr/hooks/groups/types';
import type { ReactNode } from 'react';

export type ActivationInstructionProps = {
  groupId: string;
  challenge: VerificationChallenge;
};

function groupIdToDnsDomain (groupId: string) {
  const [first, second] = groupId.split('.');

  if (!first || !second) {
    return groupId;
  }

  return `${second}.${first}`;
}

function ChecklistStep ({
  index,
  title,
  description,
}: {
  index: string;
  title: ReactNode;
  description: ReactNode;
}) {
  return (
    <div className="flex gap-4 border-t pt-4 first:border-t-0 first:pt-0">
      <div className="flex size-7 shrink-0 items-center justify-center rounded-full border text-sm font-medium">
        {index}
      </div>
      <div className="grid gap-1">
        <p className="font-medium leading-tight">{title}</p>
        <div className="text-sm text-muted-foreground">{description}</div>
      </div>
    </div>
  );
}

export function DnsActivationInstruction ({ groupId, challenge }: ActivationInstructionProps) {
  const dnsDomain = groupIdToDnsDomain(groupId);
  const tokenValue = `konfigyr-verification=${challenge.token}`;

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="text-base text-muted-foreground">
          <FormattedMessage
            defaultMessage="Add a DNS TXT record"
            description="Heading for the DNS verification checklist."
          />
        </CardTitle>
        <CardDescription className="flex items-center justify-between gap-4">
          <FormattedMessage
            defaultMessage="Follow the steps below to prove ownership of {groupId}."
            values={{ groupId }}
            description="Description for the DNS verification checklist."
          />
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <ChecklistStep
          index="1"
          title={(
            <FormattedMessage
              defaultMessage="Open your DNS provider"
              description="First step in the DNS verification checklist."
            />
          )}
          description={(
            <FormattedMessage
              defaultMessage="Sign in to wherever you manage DNS for {dnsDomain}."
              values={{ dnsDomain }}
              description="Description for the first DNS verification checklist step."
            />
          )}
        />
        <ChecklistStep
          index="2"
          title={(
            <FormattedMessage
              defaultMessage="Add a new TXT record"
              description="Second step in the DNS verification checklist."
            />
          )}
          description={(
            <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="min-w-0 flex-1 font-mono text-sm">
                {tokenValue}
              </div>
              <ClipboardButton
                text={tokenValue}
                variant="outline"
                size="sm"
                className="shrink-0"
              />
            </div>
          )}
        />
        <ChecklistStep
          index="3"
          title={(
            <FormattedMessage
              defaultMessage="Save the record"
              description="Third step in the DNS verification checklist."
            />
          )}
          description={(
            <FormattedMessage
              defaultMessage="Your provider may take a few minutes to apply the change."
              description="Description for the third DNS verification checklist step."
            />
          )}
        />
        <ChecklistStep
          index="4"
          title={(
            <FormattedMessage
              defaultMessage="Come back and verify"
              description="Fourth step in the DNS verification checklist."
            />
          )}
          description={(
            <FormattedMessage
              defaultMessage="Propagation can take up to 48 hours. You do not need to wait on this page."
              description="Description for the fourth DNS verification checklist step."
            />
          )}
        />
      </CardContent>
    </Card>
  );
}

export function SourceCodeActivationInstruction ({ groupId, challenge }: ActivationInstructionProps) {
  const { label: host, account } = sourceCodeHostLabel(groupId);
  const tokenValue = `KFGYR-${challenge.token}`;

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="text-base text-muted-foreground">
          <FormattedMessage
            defaultMessage="Create a public repository on {host}"
            values={{ host }}
            description="Heading for the source code verification checklist."
          />
        </CardTitle>
        <CardDescription className="flex items-center justify-between gap-4">
          <FormattedMessage
            defaultMessage="Follow the steps below to prove ownership of the {account} account on {host}."
            values={{ host, account }}
            description="Description for the source code verification checklist."
          />
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <ChecklistStep
          index="1"
          title={(
            <FormattedMessage
              defaultMessage="Open {host}"
              values={{ host }}
              description="First step in the source code verification checklist."
            />
          )}
          description={(
            <FormattedMessage
              defaultMessage="Sign in to {host} with account {account}."
              values={{ host, account }}
              description="Description for the first source code verification checklist step."
            />
          )}
        />
        <ChecklistStep
          index="2"
          title={(
            <FormattedMessage
              defaultMessage="Create a new public repository"
              description="Second step in the DNS verification checklist."
            />
          )}
          description={(
            <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="min-w-0 flex-1 font-mono text-sm">
                {tokenValue}
              </div>
              <ClipboardButton
                text={tokenValue}
                variant="outline"
                size="sm"
                className="shrink-0"
              />
            </div>
          )}
        />
        <ChecklistStep
          index="3"
          title={(
            <FormattedMessage
              defaultMessage="Save the repository"
              description="Third step in the source code verification checklist."
            />
          )}
          description={(
            <FormattedMessage
              defaultMessage="Create the repository and make sure it is publicly accessible."
              description="Description for the third source code verification checklist step."
            />
          )}
        />
        <ChecklistStep
          index="4"
          title={(
            <FormattedMessage
              defaultMessage="Come back and verify"
              description="Fourth step in the source code verification checklist."
            />
          )}
          description={(
            <FormattedMessage
              defaultMessage="Return here and click Verify. We will check that the public repository exists in {host} account."
              values={{ host }}
              description="Description for the fourth source code verification checklist step."
            />
          )}
        />
      </CardContent>
    </Card>
  );
}