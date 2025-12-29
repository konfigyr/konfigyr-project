import { CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useReactivateKeyset } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { CancelLabel } from '@konfigyr/components/messages';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { Button } from '@konfigyr/components/ui/button';

import type { Keyset, Namespace } from '@konfigyr/hooks/types';

export function KeysetReactivateOperation({ namespace, keyset, onCancel }: { namespace: Namespace, keyset: Keyset, onCancel: () => void }) {
  const { mutateAsync: reactivate, isSuccess, isError, error } = useReactivateKeyset(namespace.slug, keyset);

  if (isSuccess) {
    return (
      <Alert variant="default">
        <CheckCircle2Icon/>
        <AlertTitle>
          <FormattedMessage
            defaultMessage="Keyset has been successfully reactivated"
            description="Label for the alert that is shown after successful KMS keyset reactivation."
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
        <Button onClick={() => reactivate()}>
          <FormattedMessage
            defaultMessage="Reactivate"
            description="Label for the button that reactivates the selected KMS keyset."
          />
        </Button>
      </div>
    </>
  );
}
