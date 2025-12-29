import { CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useRotateKeyset } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { CancelLabel } from '@konfigyr/components/messages';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { Button } from '@konfigyr/components/ui/button';

import type { Keyset, Namespace } from '@konfigyr/hooks/types';

export function KeysetRotateOperation({ namespace, keyset, onCancel }: { namespace: Namespace, keyset: Keyset, onCancel: () => void }) {
  const { mutateAsync: rotate, isSuccess, isError, error } = useRotateKeyset(namespace.slug, keyset);

  if (isSuccess) {
    return (
      <Alert variant="default">
        <CheckCircle2Icon/>
        <AlertTitle>
          <FormattedMessage
            defaultMessage="Keyset has been successfully rotated"
            description="Label for the alert that is shown after successful keyset rotation."
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
        <Button onClick={() => rotate()}>
          <FormattedMessage
            defaultMessage="Generate new key"
            description="Label for the button that generates a new KMS key for the selected keyset."
          />
        </Button>
      </div>
    </>
  );
}
