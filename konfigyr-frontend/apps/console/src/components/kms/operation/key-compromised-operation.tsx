import { CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useCompromiseKey } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { CancelLabel, YesLabel } from '@konfigyr/components/messages';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { Button } from '@konfigyr/components/ui/button';

import type { Key, Keyset, Namespace } from '@konfigyr/hooks/types';

export function KeyCompromisedOperation({ namespace, keyset, value: key, onCancel }: {
  namespace: Namespace;
  value: Key;
  keyset: Keyset;
  onCancel: () => void;
}) {
  const { mutateAsync: compromise, isSuccess, isError, error } = useCompromiseKey(namespace.slug, keyset);

  if (isSuccess) {
    return (
      <Alert variant="default">
        <CheckCircle2Icon/>
        <AlertTitle>
          <FormattedMessage
            defaultMessage="Key has been mark as compromised."
            description="Label for the alert that is shown after KMS key has been successfully marked as compromised."
          />
        </AlertTitle>
      </Alert>
    );
  }

  return (
    <>
      {isError && (
        <ErrorState error={error} />
      )}

      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onCancel}>
          <CancelLabel />
        </Button>
        <Button onClick={() => compromise(key)}>
          <YesLabel />
        </Button>
      </div>
    </>
  );
}
