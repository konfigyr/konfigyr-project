import { Link, useLocation } from '@tanstack/react-router';
import { createLogger } from '@konfigyr/logger';

const logger = createLogger('not-found');

export function NotFound({ children }: { children?: any }) {
  const location = useLocation();

  logger.debug(`Rendering not found page for location ${location.url}`);

  return (
    <div className="space-y-2 p-2">
      <div className="text-gray-600 dark:text-gray-400">
        {children || <h3>The page you are looking for does not exist.</h3>}
      </div>
      <p className="flex items-center gap-2 flex-wrap">
        <button onClick={() => window.history.back()}>
          Go back
        </button>
        <Link to="/">
          Start Over
        </Link>
      </p>
    </div>
  );
}
