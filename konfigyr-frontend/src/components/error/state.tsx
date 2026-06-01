import { useMemo } from 'react';
import { SimpleAlert } from '@konfigyr/components/ui/alert';
import { normalizeError } from './normalize';

import type { AlertProps } from '@konfigyr/components/ui/alert';

export type ErrorStateProps = { error: Error } & Omit<AlertProps, 'title' | 'description'>;

export function ErrorState({ error, ...props }: ErrorStateProps) {
  const normalized = useMemo(() => normalizeError(error), [error]);

  return (
    <SimpleAlert
      variant="destructive"
      title={normalized.title}
      description={normalized.detail}
      {...props}
    />
  );
}
