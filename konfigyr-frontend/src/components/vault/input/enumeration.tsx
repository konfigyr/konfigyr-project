import { FormattedMessage } from 'react-intl';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/components/ui/select';
import { cn } from '@konfigyr/components/utils';

import type { InputFieldProps } from './types';

export type EnumerationFieldProps = InputFieldProps<HTMLElement, string>;

export function EnumerationField({ property, value, className, onChange }: EnumerationFieldProps) {
  return (
    <Select value={value} onValueChange={it => it && onChange?.(it)}>
      <SelectTrigger
        className={cn('text-sm font-mono w-full', className)}
        aria-label={property.schema.title || property.name}
        aria-description={property.schema.description || property.description}
      >
        <SelectValue placeholder={property.defaultValue} />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel>
            <FormattedMessage
              defaultMessage="Select an option"
              description="Label for the dropdown menu to select an option in the enumerated input field."
            />
          </SelectLabel>
          {property.schema.enum?.map(option => (
            <SelectItem key={option} value={option}>
              {option}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}
