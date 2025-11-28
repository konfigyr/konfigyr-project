import { createLogger } from '@konfigyr/logger';
import { ErrorComponent, Link, useLocation, useRouter } from '@tanstack/react-router';

import type { ErrorComponentProps } from '@tanstack/react-router';

const logger = createLogger('error');

export function ErrorBoundary({ error }: ErrorComponentProps) {
  const location = useLocation();
  const router = useRouter();

  logger.error({ error },`Unexpected error occurred while rendering page: ${location.url}`);

  return (
    <div className="container gap-6">
      <ErrorComponent error={error} />

      <div className="flex gap-2 items-center flex-wrap">
        <button onClick={() => router.invalidate()}>
          Try Again
        </button>
        <Link to="/">
          Home
        </Link>
      </div>
    </div>
  );
}
