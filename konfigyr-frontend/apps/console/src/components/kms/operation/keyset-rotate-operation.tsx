import { useState } from 'react';
import { CheckCircle2Icon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useRotateKeyset } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { CancelLabel } from '@konfigyr/components/messages';
import { Alert, AlertTitle } from '@konfigyr/components/ui/alert';
import { Button } from '@konfigyr/components/ui/button';
import {
  Field,
  FieldDescription,
  FieldLabel,
} from '@konfigyr/components/ui/field';
import { KeysetAlgorithmSelect } from '../keyset-algorithm';
import { SupportedAlgorithm } from '../supported-algorithms';

import type { Keyset, KeysetPurpose, Namespace } from '@konfigyr/hooks/types';

function SelectAlgorithmField({ id, value, purpose, onChange }: {
  id: string;
  value: string;
  purpose?: KeysetPurpose;
  onChange: (value: string) => void;
}) {
  const elementId = `input-${id}`;
  const helpTextId = `help-text-${id}`;

  return (
    <Field>
      <FieldLabel htmlFor={elementId}>
        Key algorithm
      </FieldLabel>
      <KeysetAlgorithmSelect
        id={elementId}
        value={value}
        purpose={purpose}
        detailed={true}
        aria-describedby={helpTextId}
        onChange={selected => onChange(selected || value)}
      />
      <FieldDescription id={helpTextId}>
        Defaults to the keyset's current algorithm. Only change this if you need to migrate to a different scheme.
      </FieldDescription>
    </Field>
  );
}

export function KeysetRotateOperation({ namespace, keyset, onCancel }: { namespace: Namespace, keyset: Keyset, onCancel: () => void }) {
  const [selected, setSelected] = useState<string>(keyset.algorithm);
  const { mutateAsync: rotate, isSuccess, isError, error } = useRotateKeyset(namespace.slug, keyset);
  const algorithm = SupportedAlgorithm.valueOf(keyset.algorithm);

  if (isSuccess) {
    return (
      <Alert variant="default">
        <CheckCircle2Icon/>
        <AlertTitle>
          <FormattedMessage
            defaultMessage="Keyset has been successfully rotated"
            description="Label for the alert that is shown after successful keyset rotation."
          />
        </AlertTitle>
      </Alert>
    );
  }

  return (
    <>
      {isError && (
        <ErrorState error={error} />
      )}

      <SelectAlgorithmField
        id={keyset.id}
        value={selected}
        purpose={algorithm?.purpose}
        onChange={setSelected}
      />

      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button variant="outline" onClick={onCancel}>
          <CancelLabel />
        </Button>
        <Button onClick={() => rotate({ algorithm: selected })}>
          <FormattedMessage
            defaultMessage="Generate new key"
            description="Label for the button that generates a new KMS key for the selected keyset."
          />
        </Button>
      </div>
    </>
  );
}
