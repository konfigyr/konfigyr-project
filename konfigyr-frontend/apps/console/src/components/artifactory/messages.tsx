import { FormattedMessage } from 'react-intl';

export const MissingPropertyDescriptionLabel = () => (
  <FormattedMessage
    tagName="i"
    defaultMessage="No description provided."
    description="Label used to describe a configuration property that has no description."
  />
);
