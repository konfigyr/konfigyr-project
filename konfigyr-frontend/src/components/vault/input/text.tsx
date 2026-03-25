import { Input } from '@konfigyr/components/ui/input';

import type { ComponentProps } from 'react';
import type { InputFieldProps } from './types';

export type TextFieldProps = InputFieldProps<HTMLInputElement, string> & ComponentProps<typeof Input>;

const useInputType = (format: string | undefined): string => {
  switch (format) {
    case 'date':
      return 'date';
    case 'time':
      return 'time';
    case 'date-time':
      return 'datetime-local';
    default:
      return 'text';
  }
};

export function TextField({ property, value, onChange, ...props }: TextFieldProps) {
  const type = useInputType(property.schema.format);

  return (
    <Input
      type={type}
      value={value}
      className="h-7 text-sm font-mono"
      minLength={property.schema.minLength}
      maxLength={property.schema.maxLength}
      pattern={property.schema.pattern}
      onChange={event => onChange?.(event.target.value)}
      {...props}
    />
  );
}
