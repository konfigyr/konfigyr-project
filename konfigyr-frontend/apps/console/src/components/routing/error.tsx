import { useInsertionEffect, useMemo } from 'react';
import { normalizeError } from '@konfigyr/components/error/normalize';
import { createLogger } from '@konfigyr/logger';
import { useLocation } from '@tanstack/react-router';

import {
  ErrorPage,
  ErrorPageContent,
  ErrorPageFooter,
  ErrorPageHeader,
  ErrorPageStackTrace,
} from '@konfigyr/components/error/page';

import type { ErrorComponentProps } from '@tanstack/react-router';

const logger = createLogger('error');

export function ErrorBoundary({ error, reset }: ErrorComponentProps) {
  const normalized = useMemo(() => normalizeError(error), [error]);
  const location = useLocation();

  useInsertionEffect(() => {
    logger.error({ error },`Unexpected error occurred while rendering page: ${location.href}`);
  });

  return (
    <ErrorPage>
      <ErrorPageHeader
        title={normalized.title}
        description={normalized.detail}
      />
      <ErrorPageContent>
        {process.env.NODE_ENV !== 'production' && (
          <ErrorPageStackTrace error={error} />
        )}
      </ErrorPageContent>
      <ErrorPageFooter onHome={reset} />
    </ErrorPage>
  );
}
