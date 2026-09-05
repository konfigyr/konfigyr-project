import { FormattedMessage } from 'react-intl';
import { Switch } from '@konfigyr/components/ui/switch';

import type { ComponentProps } from 'react';
import type { InputFieldProps } from './types';

export type BooleanFieldProps = InputFieldProps<HTMLElement, boolean> & ComponentProps<typeof Switch>;

export function BooleanField({ id, property, value, onChange, ...props }: BooleanFieldProps) {
  // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
  const checked = value === true;

  return (
    <div className='group inline-flex items-center gap-2' data-state={checked ? 'checked' : 'unchecked'}>
      <span
        className='group-data-[state=checked]:text-muted-foreground/70 cursor-pointer text-right text-sm font-medium'
        aria-controls={id}
        onClick={() => onChange?.(false)}
      >
        <FormattedMessage
          defaultMessage="Off"
          description="The `Off` label used in the boolean input field for vault configuration properties."
        />
      </span>
      <Switch
        id={id}
        checked={checked}
        onCheckedChange={onChange}
        {...props}
      />
      <span
        className='group-data-[state=unchecked]:text-muted-foreground/70 cursor-pointer text-right text-sm font-medium'
        aria-controls={id}
        onClick={() => onChange?.(true)}
      >
        <FormattedMessage
          defaultMessage="On"
          description="The `On` label used in the boolean input field for vault configuration properties."
        />
      </span>
    </div>
  );
}
