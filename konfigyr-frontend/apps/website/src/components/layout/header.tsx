import { FormattedMessage, defineMessages } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { CallToActionLink } from '@konfigyr/components/cta';

const messages = defineMessages({
  about: {
    defaultMessage: 'About',
    description: 'Header navigation link to the about page',
  },
  cta: {
    defaultMessage: 'Request Early Access',
    description: 'Header navigation call to action button',
  },
});

export function Header() {
  return (
    <header className="border-b">
      <div className="container max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <img src="/logo.svg" alt="" className="h-7 w-7" />
          <span className="font-heading font-semibold text-lg">Konfigyr</span>
        </Link>

        <nav className="flex items-center gap-6">
          <Link
            to="/about"
            className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
          >
            <FormattedMessage {...messages.about} />
          </Link>

          <CallToActionLink />
        </nav>
      </div>
    </header>
  );
}
