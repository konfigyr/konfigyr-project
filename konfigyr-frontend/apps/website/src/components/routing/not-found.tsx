import { FormattedMessage, defineMessages } from 'react-intl';
import { Link } from '@tanstack/react-router';

const messages = defineMessages({
  lead: {
    defaultMessage: 'Page not found',
    description: 'The not found page lead',
  },
  subtitle: {
    defaultMessage: 'The page you\'re looking for doesn\'t exist, or has moved.',
    description: 'The not found page subtitle',
  },
  home: {
    defaultMessage: 'Back to homepage',
    description: 'Link on the not found page that navigates back to the homepage',
  },
});

export function NotFound() {
  return (
    <div className="container px-4 py-24 max-w-2xl mx-auto grid gap-4 text-center">
      <p className="text-2xl font-medium text-primary">404</p>
      <h1 className="text-4xl font-heading font-semibold">
        <FormattedMessage {...messages.lead} />
      </h1>
      <p className="text-lg text-muted-foreground">
        <FormattedMessage {...messages.subtitle} />
      </p>
      <div>
        <Link to="/" className="underline underline-offset-4 hover:text-primary transition-colors">
          <FormattedMessage {...messages.home} />
        </Link>
      </div>
    </div>
  );
}
