import { FormattedMessage } from 'react-intl';
import { SimpleAlert } from '@konfigyr/components/alert';

type ConfigurationImporterStatusProps = {
  isParsing: boolean;
  isError: boolean;
  error?: Error;
  valid: number;
  invalid: number;
};

/**
 * Renders the current parser and validation status.
 */
export function ConfigurationImporterStatus ({ valid, invalid, isParsing, isError, error }: ConfigurationImporterStatusProps) {
  const hasProperties = !!valid || !!invalid;
  return (
    <>
      {isParsing && (
        <ReadingConfigurationAlert />
      )}

      {isError && (
        <ParsingErrorAlert error={error?.message}/>
      )}

      {!hasProperties && !isError && !isParsing && (
        <EmptyAlert />
      )}

      {hasProperties && !isError && (
        <>
          <InvalidPropertiesAlert count={invalid}/>
          <ValidPropertiesAlert count={valid}/>
        </>
      )}
    </>
  );
}

/**
 * Shows the warning state for invalid imported properties.
 */
function InvalidPropertiesAlert ({ count }: { count: number }) {
  if (!count) {
    return;
  }
  return (
    <SimpleAlert
      variant="warning"
      title={(
        <FormattedMessage
          defaultMessage="Review imported properties"
          description="Title shown when imported configuration contains validation errors and requires user review before import."
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Found {count} invalid properties. Continue with valid properties only, or import all and resolve issues afterward."
          description="Description shown in the import review warning when imported configuration contains invalid values."
          values={{ count }}
        />
      )}
    />
  );
}

/**
 * Shows the success state for valid imported properties.
 */
function ValidPropertiesAlert ({ count }: { count: number }) {
  if (!count) {
    return;
  }
  return (
    <SimpleAlert
      variant="default"
      title={(
        <FormattedMessage
          defaultMessage="Ready for import"
          description="Title shown when valiad and ready for import properties"
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="The configuration was parsed, and {count} new configuration properties are available for import."
          description="Success message shown when the configuration is parsed and new properties are available for import."
          values={{ count }}
        />
      )}
    />
  );
}

/**
 * Shows the empty-state message when no properties were parsed.
 */
function EmptyAlert () {
  return (
    <SimpleAlert
      variant="default"
      title={(
        <FormattedMessage
          defaultMessage="No configuration properties for import"
          description="Empty state description shown when parsed configuration has no properties."
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Your configuration is empty"
          description="Empty state title shown when parsed configuration has no properties."
        />
      )}
    />
  );
}

/**
 * Shows the loading state while configuration is being parsed.
 */
function ReadingConfigurationAlert () {
  return (
    <SimpleAlert
      variant="default"
      title={(
        <FormattedMessage
          defaultMessage="Reading configuration"
          description="Status title shown while configuration file is being parsed."
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Reading your configuration"
          description="Status description shown while configuration file is being parsed"
        />
      )}
    />
  );
}

/**
 * Shows the parse failure message.
 */
function ParsingErrorAlert ({ error }: { error?: string }) {
  return (
    <SimpleAlert
      variant="destructive"
      title={(
        <FormattedMessage
          defaultMessage="Could not import your configuration"
          description="Error message shown when configuration parsing fails"
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Error: {error}"
          description="Error message shown when configuration parsing fails'"
          values={{ error }}
        />
      )}
    />
  );
}
