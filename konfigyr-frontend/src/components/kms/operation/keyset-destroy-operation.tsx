import { CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useDestroyKeyset } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { CancelLabel } from '@konfigyr/components/messages';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { Button } from '@konfigyr/components/ui/button';

import type { Keyset, Namespace } from '@konfigyr/hooks/types';

export function KeysetDestroyOperation({ namespace, keyset, onCancel }: { namespace: Namespace, keyset: Keyset, onCancel: () => void }) {
  const { mutateAsync: destroy, isSuccess, isError, error } = useDestroyKeyset(namespace.slug, keyset);

  if (isSuccess) {
    return (
      <Alert variant="default">
        <CheckCircle2Icon/>
        <AlertTitle>
          <FormattedMessage
            defaultMessage="Keyset has been successfully destroyed"
            description="Label for the alert that is shown when keyset was successfully destroyed."
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
        <Button variant="destructive" onClick={() => destroy()}>
          <FormattedMessage
            defaultMessage="Yes, I am sure"
            description="Label for the button that confirms the destruction of a keyset."
          />
        </Button>
      </div>
    </>
  );
}
