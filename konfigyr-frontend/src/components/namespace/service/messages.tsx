import { FormattedMessage } from 'react-intl';

export function ServiceNameLabel() {
  return (
    <FormattedMessage
      defaultMessage="Display name"
      description="The form label used in the service forms to define the service display name input field"
    />
  );
}

export function ServiceDescriptionLabel() {
  return (
    <FormattedMessage
      defaultMessage="Description"
      description="The form label used in the service forms to define the service description input field"
    />
  );
}

export function ServiceDescriptionHelpText() {
  return (
    <FormattedMessage
      defaultMessage="Add any internal notes about this service."
      description="The form help text used in the service forms to define the service description input field"
    />
  );
}
