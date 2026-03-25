import { Input } from '@konfigyr/components/ui/input';

import type { ComponentProps } from 'react';
import type { InputFieldProps } from './types';

export type NumericFieldProps = InputFieldProps<HTMLInputElement, number> & ComponentProps<typeof Input>;

export function NumericField({ property, value, onChange, ...props }: NumericFieldProps) {
  return (
    <Input
      type="number"
      value={value}
      className="h-7 text-sm font-mono"
      min={property.schema.minimum}
      max={property.schema.maximum}
      onChange={event => onChange?.(event.target.valueAsNumber)}
      {...props}
    />
  );
}
