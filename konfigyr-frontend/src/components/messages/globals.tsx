import { FormattedMessage } from 'react-intl';

import type { ComponentProps } from 'react';

export const KonfigyrTitleMessage = () => (
  <FormattedMessage
    id="globals.title"
    defaultMessage="Konfigyr"
    description="The title of the Konfigyr application"
  />
);

export const KonfigyrLeadMessage = () => (
  <FormattedMessage
    id="globals.lead"
    defaultMessage="Configuration made easy."
    description="The lead message of the Konfigyr application"
  />
);

export const GeneralErrorTitle = () => (
  <FormattedMessage
    id="errors.title"
    defaultMessage="Unexpected server error occurred"
    description="Used to provide a default error message title"
  />
);

export const GeneralErrorDetail = () => (
  <FormattedMessage
    id="errors.detail"
    defaultMessage="We could not process your request due to a runtime error. Please try again and if the problem persists, please get in touch with our support team."
    description="Used to provide a default error message detail"
  />
);

export const GeneralErrorLink = ({ children, ...props }: ComponentProps<'a'>) => (
  <a target="_blank" rel="noopener noreferrer" {...props}>
    <FormattedMessage
      id="errors.link"
      defaultMessage="Find out more here"
      description="Used as a label for a link that leads to the error documentation"
    />
    {children}
  </a>
);

export const OAuthErrorTitle = () => (
  <FormattedMessage
    id="errors.oauth.title"
    defaultMessage="Authorization error"
    description="Used to provide a default OAuth error message title"
  />
);

export const OAuthErrorDetail = () => (
  <FormattedMessage
    id="errors.oauth.detail"
    defaultMessage="Unexpected server error occurred while logging you in. Please try again and if the problem persists, please get in touch with our support team."
    description="Used to provide a default OAuth error message detail"
  />
);

export const ContactSupport = () => (
  <FormattedMessage
    id="globals.contact.support"
    defaultMessage="Contact our support team"
    description="The label for a link that leads to the support page"
  />
);
