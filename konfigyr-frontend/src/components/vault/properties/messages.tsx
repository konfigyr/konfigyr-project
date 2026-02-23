import { FormattedMessage } from 'react-intl';

export const PropertyNameLabel = () => (
  <FormattedMessage
    defaultMessage="Property name"
    description="Label used to describe a configuration property name. Used in table headers and forms."
  />
);

export const PropertyValueLabel = () => (
  <FormattedMessage
    defaultMessage="Value"
    description="Label used to describe a configuration property value. Used in table headers and forms."
  />
);

export const MissingPropertyDescriptionLabel = () => (
  <FormattedMessage
    defaultMessage="No description provided."
    description="Label used to describe a configuration property that has no description."
  />
);

export const AddPropertyLabel = () => (
  <FormattedMessage
    defaultMessage="Add property"
    description="Label used in action links or buttons that would add a configuration property to the changeset history."
  />
);

export const RestorePropertyLabel = () => (
  <FormattedMessage
    defaultMessage="Restore property"
    description="Label used in action links or buttons that would restore a configuration property from the changeset history."
  />
);
