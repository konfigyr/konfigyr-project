import { useId } from 'react';
import { useIntl } from 'react-intl';
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldLabel,
} from '@konfigyr/components/ui/field';
import {
  RadioGroup,
  RadioGroupItem,
} from '@konfigyr/components/ui/radio-group';

import type { Profile } from '@konfigyr/hooks/types';

export const usePolicyLabel = (policy: Profile['policy']) => {
  const intl = useIntl();

  switch (policy) {
    case 'UNPROTECTED': return intl.formatMessage({
      defaultMessage: 'Unprotected',
      description: 'Label for the unprotected profile policy in the profile policy picker.',
    });
    case 'PROTECTED': return intl.formatMessage({
      defaultMessage: 'Protected',
      description: 'Label for the protected profile policy in the profile policy picker.',
    });
    case 'LOCKED': return intl.formatMessage({
      defaultMessage: 'Locked',
      description: 'Label for the locked profile policy in the profile policy picker.',
    });
  }
};

export const usePolicyDescription = (policy: Profile['policy']) => {
  const intl = useIntl();

  switch (policy) {
    case 'UNPROTECTED':
      return intl.formatMessage({
        defaultMessage: 'Changesets can be applied directly without approval. Suitable for development or non-critical environments.',
        description: 'Description for the unprotected profile policy in the profile policy picker.',
      });
    case 'PROTECTED':
      return intl.formatMessage({
        defaultMessage: 'Changesets must be reviewed and approved before being applied. Recommended for staging or production environments.',
        description: 'Description for the protected profile policy in the profile policy picker.',
      });
    case 'LOCKED':
      return intl.formatMessage({
        defaultMessage: 'Changeset in locked state are marked as read only.',
        description: 'Description for the protected locked policy in the profile policy picker.',
      });
  }
};

function PolicyOption({ value }: { value: Profile['policy'] }) {
  const id = `profile-policy-${value}-${useId()}`;
  const label = usePolicyLabel(value);
  const description = usePolicyDescription(value);

  return (
    <Field orientation="horizontal">
      <RadioGroupItem id={id} value={value} />
      <FieldContent>
        <FieldLabel htmlFor={id}>
          {label}
        </FieldLabel>
        <FieldDescription>
          {description}
        </FieldDescription>
      </FieldContent>
    </Field>
  );
}

export function PolicyPicker({ options, value, onChange}: {
  options: Array<Profile['policy']>,
  value?: Profile['policy'],
  onChange: (value: Profile['policy']) => void
}) {
  return (
    <RadioGroup value={value} onValueChange={onChange}>
      {options.map(option => (
        <PolicyOption
          key={option}
          value={option}
        />
      ))}
    </RadioGroup>
  );
}
