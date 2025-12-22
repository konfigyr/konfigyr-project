import { useMemo } from 'react';
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@konfigyr/components/ui/alert';
import { normalizeError } from './normalize';

export function ErrorState({ error, ...props }: { error: Error } & React.ComponentProps<typeof Alert>) {
  const normalized = useMemo(() => normalizeError(error), [error]);

  return (
    <Alert variant="destructive" {...props}>
      <AlertTitle>{normalized.title}</AlertTitle>
      <AlertDescription>{normalized.detail}</AlertDescription>
    </Alert>
  );
}
