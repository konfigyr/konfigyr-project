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

import type { InputFieldProps } from './types';

export type EnumerationFieldProps = InputFieldProps<HTMLElement, string>;

export function EnumerationField({ property, value, onChange }: EnumerationFieldProps) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger
        className="h-7 text-sm font-mono w-full"
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
