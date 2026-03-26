import { useCallback, useMemo, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { PlusIcon } from 'lucide-react';
import {
  Combobox,
  ComboboxChip,
  ComboboxChips,
  ComboboxChipsInput,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxItem,
  ComboboxList,
  ComboboxValue,
  useComboboxAnchor,
} from '@konfigyr/components/ui/combobox';

import type { ComponentProps, KeyboardEvent, ReactNode } from 'react';
import type { InputFieldProps, SchemaHint } from './types';

export type ArrayFieldProps = InputFieldProps<HTMLElement, Array<string>> & ComponentProps<typeof Combobox>;

type Item = {
  id: string,
  type: 'enum' | 'example' | 'input' | 'value',
  value: string,
  label: ReactNode,
};

const addItemsFor = (items: Array<Item>, type: Item['type'], values?: Array<string | SchemaHint>) => {
  if (Array.isArray(values) && values.length > 0) {
    return values.reduce(
      (state, value) => {
        if (typeof value === 'string') {
          if (state.some(it => it.value === value)) {
            return state;
          }
          state.push({ id: type + ':' + value, type, value, label: value });
        } else {
          if (state.some(it => it.value === value.value)) {
            return state;
          }
          state.push({ id: type + ':' + value.value, type, ...value });
        }
        return state;
      },
      items,
    );
  }
  return items;
};

export function ArrayField({ property, value, hints, onChange, onBlur, onKeyDown }: ArrayFieldProps) {
  const anchor = useComboboxAnchor();
  const [input, onInputChange] = useState('');
  const [highlighted, onHighlightedChange] = useState(false);

  const items = useMemo(() => {
    const state = addItemsFor([], 'value', value);
    addItemsFor(state, 'example', hints);
    addItemsFor(state, 'enum', property.schema.items?.enum);
    addItemsFor(state, 'example', property.schema.items?.examples);
    return state;
  }, [value, hints, property.schema.items]);

  if (!property.schema.items?.enum && input && !items.some(it => it.value === input)) {
    items.push({ id: `create:${input}`, type: 'input', value: input, label: input });
  }

  const onInputKeyDown = useCallback((event: KeyboardEvent<HTMLInputElement>) => {
    if (!highlighted && event.key === 'Enter') {
      if (input.trim().length === 0) {
        onKeyDown?.(event);
      }
    }
  }, [input, highlighted, onKeyDown]);

  return (
    <Combobox
      multiple
      autoHighlight
      items={items}
      value={value}
      onValueChange={onChange}
      inputValue={input}
      onInputValueChange={onInputChange}
      onItemHighlighted={it => onHighlightedChange(!!it)}
    >
      <ComboboxChips ref={anchor} className="w-full max-w-xs">
        <ComboboxValue>
          {(values) => (
            <>
              {values.map((it: string) => (
                <ComboboxChip key={it}>{it}</ComboboxChip>
              ))}
              <ComboboxChipsInput
                autoFocus
                onBlur={onBlur}
                onKeyDown={onInputKeyDown}
                name={property.schema.title || property.name}
                aria-label={property.schema.title || property.name}
                aria-description={property.schema.description || property.description}
              />
            </>
          )}
        </ComboboxValue>
      </ComboboxChips>
      <ComboboxContent anchor={anchor}>
        <ComboboxEmpty>
          <FormattedMessage
            defaultMessage="No matching items found"
            description="Message displayed when no items are found in the combobox for an array like configuration property."
          />
        </ComboboxEmpty>
        <ComboboxList>
          {(item: Item) => (
            <ComboboxItem key={item.id} value={item.value}>
              {item.type === 'input' && (<PlusIcon />)}
              {item.label}
            </ComboboxItem>
          )}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  );
}
