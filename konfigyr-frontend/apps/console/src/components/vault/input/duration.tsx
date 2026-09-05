import { useCallback, useState } from 'react';
import { ChevronDownIcon } from 'lucide-react';
import { DurationUnit } from '@konfigyr/hooks/transforms';
import { DurationUnitLabel, SelectDurationUnitLabel } from '@konfigyr/components/messages/duration';
import { Kbd } from '@konfigyr/components/ui/kbd';
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from '@konfigyr/components/ui/input-group';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';
import type { Duration } from '@konfigyr/hooks/transforms';
import type { InputFieldProps } from './types';

export type DurationFieldProps = InputFieldProps<HTMLInputElement, Duration> & ComponentProps<typeof InputGroupInput>;

export function DurationField({ property, value, className, onChange, onBlur, ...props }: DurationFieldProps) {
  const [inputValue, setInputValue] = useState(value?.value);
  const [open, setOpen] = useState(false);

  const onValueChange = useCallback((val: number) => {
    setInputValue(val);

    if (!isNaN(val) && onChange) {
      onChange({ unit: value?.unit ?? DurationUnit.MILLISECONDS, value: val });
    }
  }, [value, onChange]);

  const onUnitChange = useCallback((unit: string) => {
    if (onChange) {
      onChange({ unit: unit as DurationUnit, value: value?.value ?? 0 });
    }
  }, [value, onChange]);

  return (
    <InputGroup className={className}>
      <InputGroupInput
        type="number"
        value={inputValue}
        className="text-sm font-mono"
        onChange={event => onValueChange(event.target.valueAsNumber)}
        {...props}
      />
      <InputGroupAddon align="inline-end">
        <DropdownMenu open={open} onOpenChange={setOpen}>
          <DropdownMenuTrigger
            render={
              <InputGroupButton variant="ghost" size="sm" className="h-5 text-xs font-semibold">
                <code>{value?.unit || DurationUnit.MILLISECONDS}</code>
                <ChevronDownIcon className={cn('size-3 transition', open && 'rotate-180')} />
              </InputGroupButton>
            }
          />
          <DropdownMenuContent className="min-w-42" align="end">
            <DropdownMenuGroup>
              <DropdownMenuLabel>
                <SelectDurationUnitLabel />
              </DropdownMenuLabel>
              <DropdownMenuRadioGroup
                value={value?.unit || DurationUnit.MILLISECONDS}
                onValueChange={onUnitChange}
              >
                {Object.values(DurationUnit).map(unit => (
                  <DropdownMenuRadioItem key={unit} value={unit}>
                    <Kbd aria-hidden>{unit}</Kbd>
                    <DurationUnitLabel unit={unit} />
                  </DropdownMenuRadioItem>
                ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </InputGroupAddon>
    </InputGroup>
  );
}
