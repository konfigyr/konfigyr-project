import { FormattedMessage, defineMessages } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { callToActionMessage } from '@konfigyr/components/cta';

const messages = defineMessages({
  copyright: {
    defaultMessage: '© {year} EBF-EDV Beratung Föllmer GmbH',
    description: 'Footer copyright line naming the legal entity operating Konfigyr',
  },
  about: {
    defaultMessage: 'About',
    description: 'Footer navigation link to the about page',
  },
  privacy: {
    defaultMessage: 'Privacy',
    description: 'Footer navigation link to the privacy policy page',
  },
  imprint: {
    defaultMessage: 'Imprint',
    description: 'Footer navigation link to the imprint page',
  },
});

export function Footer() {
  return (
    <footer className="border-t">
      <div className="container max-w-6xl mx-auto px-4 py-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-muted-foreground">
        <div className="flex items-center gap-2">
          <img src="/logo.svg" alt="" className="h-5 w-5" />
          <span>
            <FormattedMessage {...messages.copyright} values={{ year: String(new Date().getFullYear()) }} />
          </span>
        </div>

        <nav className="flex items-center gap-6">

          <Link to="/about" className="hover:text-foreground transition-colors">
            <FormattedMessage {...messages.about} />
          </Link>
          <Link to="/privacy" className="hover:text-foreground transition-colors">
            <FormattedMessage {...messages.privacy} />
          </Link>
          <Link to="/imprint" className="hover:text-foreground transition-colors">
            <FormattedMessage {...messages.imprint} />
          </Link>
          <Link to="/early-access" className="hover:text-foreground transition-colors">
            <FormattedMessage {...callToActionMessage} />
          </Link>
        </nav>
      </div>
    </footer>
  );
}
