import { FormattedMessage } from 'react-intl';
import { Link, useLocation } from '@tanstack/react-router';
import { HomeLabel } from '@konfigyr/components/messages';
import { createLogger } from '@konfigyr/logger';

const logger = createLogger('not-found');

export function NotFound({ children }: { children?: any }) {
  const location = useLocation();

  logger.debug(`Rendering not found page for location ${location.href}`);

  return (
    <div className="container px-4 my-10 flex items-center">
      <div className="grid gap-4">
        <div className="text-2xl font-medium text-primary">
          404
        </div>
        <h3 className="text-4xl font-medium">
          <FormattedMessage
            defaultMessage="Resource Not Found"
            description="Title that is shown on the not found page when user navigates to a non-existing page."
          />
        </h3>
        <p className="text-lg text-muted-foreground">
          <FormattedMessage
            defaultMessage="The requested URI could not be resolved. Please check the endpoint syntax for typos or verify that the resource hasn't been garbage collected."
            description="Description that is shown on the not found page when user navigates to a non-existing page."
          />
        </p>
        {children}
        <div className="flex items-center gap-2">
          <button
            className="underline underline-offset-2 hover:text-primary transition-colors cursor-pointer"
            onClick={() => window.history.back()}
          >
            <FormattedMessage
              defaultMessage="Go back"
              description="Label on the button that navigates the user back to the previous page."
            />
          </button>

          <Link to="/" className="underline underline-offset-2 hover:text-primary transition-colors">
            <HomeLabel />
          </Link>
        </div>
      </div>
    </div>
  );
}
