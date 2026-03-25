import { Switch } from '@konfigyr/components/ui/switch';

import type { ComponentProps } from 'react';
import type { InputFieldProps } from './types';

export type BooleanFieldProps = InputFieldProps<HTMLElement, boolean> & ComponentProps<typeof Switch>;

export function BooleanField({ property, value, onChange, ...props }: BooleanFieldProps) {
  return (
    <Switch
      checked={value}
      onCheckedChange={onChange}
      {...props}
    />
  );
}
