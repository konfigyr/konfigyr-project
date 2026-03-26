import { useCallback, useState } from 'react';
import { ChevronDownIcon } from 'lucide-react';
import { DataUnit } from '@konfigyr/hooks/transforms';
import { DataSizeUnitLabel, SelectDataSizeUnitLabel } from '@konfigyr/components/messages/data-size';
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

import type { ComponentProps, FocusEvent } from 'react';
import type { DataSize } from '@konfigyr/hooks/transforms';
import type { InputFieldProps } from './types';

export type DataSizeFieldProps = InputFieldProps<HTMLInputElement, DataSize> & ComponentProps<typeof InputGroupInput>;

export function DataSizeField({ property, value, onChange, onBlur, ...props }: DataSizeFieldProps) {
  const [inputValue, setInputValue] = useState(value?.value);
  const [open, setOpen] = useState(false);

  const onValueChange = useCallback((val: number) => {
    setInputValue(val);

    if (!isNaN(val) && onChange) {
      onChange({ unit: value?.unit ?? DataUnit.BYTES, value: val });
    }
  }, [value, onChange]);

  const onUnitChange = useCallback((unit: string) => {
    if (onChange) {
      onChange({ unit: unit as DataUnit, value: value?.value ?? 0 });
    }
  }, [value, onChange]);

  const onFocusOut = (event: FocusEvent<HTMLInputElement>) => {
    if (!open && typeof onBlur === 'function') {
      onBlur(event);
    }
  };

  return (
    <InputGroup className="h-7">
      <InputGroupInput
        type="number"
        value={inputValue}
        className="text-sm font-mono"
        onChange={event => onValueChange(event.target.valueAsNumber)}
        onBlur={onFocusOut}
        {...props}
      />
      <InputGroupAddon align="inline-end">
        <DropdownMenu open={open} onOpenChange={setOpen}>
          <DropdownMenuTrigger
            render={
              <InputGroupButton variant="ghost" size="xs" className="h-5 text-xs font-semibold">
                <code>{value?.unit || DataUnit.BYTES}</code>
                <ChevronDownIcon className={cn('size-3 transition', open && 'rotate-180')} />
              </InputGroupButton>
            }
          />
          <DropdownMenuContent align="end">
            <DropdownMenuGroup>
              <DropdownMenuLabel>
                <SelectDataSizeUnitLabel />
              </DropdownMenuLabel>
              <DropdownMenuRadioGroup
                value={value?.unit || DataUnit.BYTES}
                onValueChange={onUnitChange}
              >
                {Object.values(DataUnit).map(unit => (
                  <DropdownMenuRadioItem key={unit} value={unit}>
                    <Kbd aria-hidden>{unit}</Kbd>
                    <DataSizeUnitLabel unit={unit} />
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
