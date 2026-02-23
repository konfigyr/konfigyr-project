import { FormattedMessage } from 'react-intl';

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

type ExpirationDateLabelProps = {
  expiresAt?: string | null;
};

export function CreateExpirationDateLabel({ expiresAt }: ExpirationDateLabelProps) {
  const hasExpirationDate = !!expiresAt;
  return (
    <>
      { hasExpirationDate ?
        <FormattedMessage
          defaultMessage="Expires on {date}"
          description="Expiration date for the application"
          values={{
            date: new Intl.DateTimeFormat(navigator.language, {
              weekday: 'short',
              month: 'short',
              day: '2-digit',
              year: 'numeric',
            }).format(new Date(expiresAt)),
          }}
        /> :
        <span className="text-orange-500">
          <FormattedMessage
            defaultMessage="This application has no expiration date"
            description="Indicates that the application does not expire"
          />
        </span>
      }
    </>
  );
}
