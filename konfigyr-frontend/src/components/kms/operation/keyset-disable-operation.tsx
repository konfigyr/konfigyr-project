import { CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useDisableKeyset } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { CancelLabel } from '@konfigyr/components/messages';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { Button } from '@konfigyr/components/ui/button';

import type { Keyset, Namespace } from '@konfigyr/hooks/types';

export function KeysetDisableOperation({ namespace, keyset, onCancel }: { namespace: Namespace, keyset: Keyset, onCancel: () => void }) {
  const { mutateAsync: disable, isSuccess, isError, error } = useDisableKeyset(namespace.slug, keyset);

  if (isSuccess) {
    return (
      <Alert variant="default">
        <CheckCircle2Icon/>
        <AlertTitle>
          <FormattedMessage
            defaultMessage="Keyset has been successfully disabled"
            description="Label for the alert that is shown when keyset was successfully disabled."
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

      <p className="text-muted-foreground text-sm leading-normal font-normal text-balance">
        <FormattedMessage
          defaultMessage="Active services using this keyset will encounter errors. Please migrate to a new keyset to avoid interruptions."
          description="Label for the confirmation dialog that is shown when user tries to disable a keyset."
        />
      </p>

      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onCancel}>
          <CancelLabel />
        </Button>
        <Button variant="destructive" onClick={() => disable()}>
          <FormattedMessage
            defaultMessage="I understand the risks"
            description="Label for the button that confirms the keyset disabling."
          />
        </Button>
      </div>
    </>
  );
}
