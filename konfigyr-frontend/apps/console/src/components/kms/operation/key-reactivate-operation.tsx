import { CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useReactivateKey } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { CancelLabel } from '@konfigyr/components/messages';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { Button } from '@konfigyr/components/ui/button';

import type { Key, Keyset, Namespace } from '@konfigyr/hooks/types';

export function KeyReactivateOperation({ namespace, keyset, value: key, onCancel }: {
  namespace: Namespace;
  value: Key;
  keyset: Keyset;
  onCancel: () => void;
}) {
  const { mutateAsync: reactivate, isSuccess, isError, error } = useReactivateKey(namespace.slug, keyset);

  if (isSuccess) {
    return (
      <Alert variant="default">
        <CheckCircle2Icon/>
        <AlertTitle>
          <FormattedMessage
            defaultMessage="Key has been successfully reactivated"
            description="Label for the alert that is shown after successful KMS key reactivation."
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
        <Button onClick={() => reactivate(key)}>
          <FormattedMessage
            defaultMessage="Reactivate"
            description="Label for the button that reactivates the selected key within a KMS keyset."
          />
        </Button>
      </div>
    </>
  );
}
