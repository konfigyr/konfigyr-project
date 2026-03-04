import { FormattedMessage, useIntl } from 'react-intl';

export function CreateNamespaceApplicationLabel() {
  return (
    <FormattedMessage
      defaultMessage="Create application"
      description="Create namespace application label text"
    />
  );
}

export function UpdateNamespaceApplicationLabel() {
  return (
    <FormattedMessage
      defaultMessage="Update application"
      description="Update namespace application label text"
    />
  );
}

export function DeleteNamespaceApplicationLabel() {
  return (
    <FormattedMessage
      defaultMessage="Delete application"
      description="Button label that triggers application delete confirmation dialog when clicked"
    />
  );
}

export function CreateExpirationDateLabel({ expiresAt }: { expiresAt?: string | Date | number | null }) {
  const intl = useIntl();

  if (!expiresAt) {
    return (
      <FormattedMessage
        defaultMessage="This application has no expiration date"
        description="Indicates that the application does not expire"
      />
    );
  }

  return (
    <FormattedMessage
      defaultMessage="Expires on {date}"
      description="Expiration date for the application"
      values={{
        date: intl.formatDate(expiresAt, {
          weekday: 'short',
          month: 'short',
          day: '2-digit',
          year: 'numeric',
        }),
      }}
    />
  );
}
