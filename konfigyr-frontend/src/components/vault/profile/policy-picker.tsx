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
    case 'IMMUTABLE': return intl.formatMessage({
      defaultMessage: 'Read-only',
      description: 'Label for the immutable read-only profile policy in the profile policy picker.',
    });
  }
};

export const usePolicyDescription = (policy: Profile['policy']) => {
  const intl = useIntl();

  switch (policy) {
    case 'UNPROTECTED':
      return intl.formatMessage({
        defaultMessage: 'Changes can be applied directly once a changeset is submitted, without requiring explicit approval. This policy is intended for non-critical environments where rapid iteration is prioritized over strict governance. Typical use cases would include local development or test environments.',
        description: 'Description for the unprotected profile policy in the profile policy picker.',
      });
    case 'PROTECTED':
      return intl.formatMessage({
        defaultMessage: 'Changesets targeting a protected profile must go through a review and approval process. Direct application of changes is not permitted. Recommended for staging or production environments.',
        description: 'Description for the protected profile policy in the profile policy picker.',
      });
    case 'IMMUTABLE':
      return intl.formatMessage({
        defaultMessage: 'Changeset in locked state are marked as read only. This policy is intended for frozen, deprecated, or compliance-bound environments where configuration drift must be prevented entirely.',
        description: 'Description for the immutable policy in the profile policy picker.',
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
        <FieldDescription className="whitespace-pre-line">
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
