import { Input } from '@konfigyr/components/ui/input';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';
import type { InputFieldProps } from './types';

export type NumericFieldProps = InputFieldProps<HTMLInputElement, number> & ComponentProps<typeof Input>;

export function NumericField({ property, value, className, onChange, ...props }: NumericFieldProps) {
  return (
    <Input
      type="number"
      value={value}
      className={cn('text-sm font-mono', className)}
      min={property.schema.minimum}
      max={property.schema.maximum}
      onChange={event => onChange?.(event.target.valueAsNumber)}
      {...props}
    />
  );
}
