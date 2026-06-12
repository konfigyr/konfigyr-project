import { Label } from '@konfigyr/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@konfigyr/components/ui/radio-group';
import { cn } from '@konfigyr/components/utils';
import { ApplicationTypeInfo } from './application-type';

import type { NamespaceApplicationType } from '@konfigyr/hooks/types';

const APPLICATION_TYPES: Array<NamespaceApplicationType> = ['SERVICE_ACCOUNT', 'AGENT', 'WORKLOAD'];

export function ApplicationTypeSelector({ value, onChange }: {
  value?: NamespaceApplicationType;
  onChange: (type: NamespaceApplicationType) => void;
}) {
  return (
    <RadioGroup value={value ?? ''} onValueChange={(v) => onChange(v as NamespaceApplicationType)}>
      {APPLICATION_TYPES.map((type) => {
        const selected = value === type;
        return (
          <Label
            key={type}
            className={cn(
              'cursor-pointer border gap-4 rounded-xl p-4 transition-colors',
              selected ? 'border-primary bg-primary/5 dark:bg-primary/10' : 'border-transparent hover:bg-muted/50',
            )}
          >
            <RadioGroupItem value={type} />
            <ApplicationTypeInfo
              type={type}
              selected={selected}
            />
          </Label>
        );
      })}
    </RadioGroup>
  );
}
