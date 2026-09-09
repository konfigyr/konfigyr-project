import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/ui/components/select';

import type { ComponentProps, ReactNode } from 'react';

export const TEAM_SIZE_OPTIONS = ['1-10', '11-50', '51-200', '201-1000', '1000+'] as const;

export type TeamSize = (typeof TEAM_SIZE_OPTIONS)[number];

export function TeamSizeSelect({ value, placeholder, onChange, ...props }: {
  value?: TeamSize | '',
  placeholder?: string | ReactNode,
  onChange?: (value: TeamSize) => void,
} & Omit<ComponentProps<typeof SelectTrigger>, 'onChange'>) {
  return (
    <Select value={value || undefined} onValueChange={next => onChange?.(next as TeamSize)}>
      <SelectTrigger {...props}>
        <SelectValue>
          {value || placeholder}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {TEAM_SIZE_OPTIONS.map(option => (
          <SelectItem key={option} value={option}>{option}</SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
