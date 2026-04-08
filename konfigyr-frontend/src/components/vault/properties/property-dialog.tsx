import {
  startTransition,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import {
  ChevronRightIcon,
  ListPlusIcon,
  PlusIcon,
} from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useDebounce } from 'use-debounce';
import {
  filterPropertyDescriptors,
  splitSearchTerm,
} from '@konfigyr/hooks';
import { useJsonSchemeTransform } from '@konfigyr/hooks/artifactory/hooks';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import { PropertyDefaultValue } from '@konfigyr/components/artifactory/property-default-value';
import { PropertyDeprecation, PropertyDeprecationAlert } from '@konfigyr/components/artifactory/property-deprecation';
import { PropertyDescription } from '@konfigyr/components/artifactory/property-description';
import { PropertySchema } from '@konfigyr/components/artifactory/property-schema';
import { InputField } from '@konfigyr/components/vault/input';
import { Button } from '@konfigyr/components/ui/button';
import {
  Combobox,
  ComboboxContent,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from '@konfigyr/components/ui/combobox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/components/ui/dialog';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from '@konfigyr/components/ui/field';
import { Kbd, KbdGroup } from '@konfigyr/components/ui/kbd';
import { Label } from '@konfigyr/components/ui/label';
import {
  AddPropertyLabel,
  PropertyNameLabel,
  PropertyValueLabel,
} from './messages';

import type { KeyboardEvent } from 'react';
import type { ComboboxRoot } from '@base-ui/react/combobox';
import type {
  ChangesetState,
  ConfigurationProperty,
  ConfigurationPropertyValue,
  PropertyDescriptor,
  ServiceCatalog,
  ServiceCatalogProperty,
} from '@konfigyr/hooks/types';

function resolveDefaultPropertyValue(property: PropertyDescriptor): string {
  const { schema, defaultValue } = property;

  if (typeof defaultValue === 'string') {
    return defaultValue;
  }

  switch (schema.type) {
    case 'boolean':
      return 'false';
    case 'string':
      if (schema.format === 'data-size') {
        return '1K';
      }
      if (schema.format === 'duration') {
        return '1s';
      }
      return '';
    default:
      return '';
  }
}

const useConditionalFilter = (
  properties: Array<ServiceCatalogProperty>,
  condition: boolean,
  term?: string,
) => {
  const [results, setResults] = useState<Array<PropertyDescriptor>>([]);
  const [debouncedTerm] = useDebounce(term, 200);

  return useMemo(() => {
    // run the filter only if the condition is true, otherwise return the cached results.
    if (condition) {
      const filtered = filterPropertyDescriptors(
        properties,
        splitSearchTerm(debouncedTerm),
      );

      setResults(filtered);
      return filtered;
    }
    return results;
  }, [properties, condition, debouncedTerm]);
};

function PropertyDescriptorItem({ property }: { property: PropertyDescriptor }) {
  return (
    <ComboboxItem value={property} className="flex-col items-start gap-1" aria-label={property.name}>
      <div className="flex w-full gap-1 justify-between items-center">
        <div className="flex gap-1 justify-center items-center">
          <PropertyName className="text-xs font-bolder" value={property.name} />
          <PropertyDeprecation size="sm" deprecation={property.deprecation} />
          {property.schema.type === 'object' && (
            <ChevronRightIcon className="size-3" />
          )}
        </div>

        <PropertySchema value={property.schema} variant="badge" />
      </div>
      <PropertyDescription value={property.description} />
      <PropertyDefaultValue value={property.defaultValue} variant="labeled" />
    </ComboboxItem>
  );
}

function PropertyDescriptorInput({ catalog, onChange }: {
  catalog: ServiceCatalog,
  onChange: (value: PropertyDescriptor | null) => void,
}) {
  const [name, onNameChange] = useState('');
  const [selection, onSelectionChange] = useState<PropertyDescriptor | null>(null);
  const [candidate, onCandidateChange] = useState<PropertyDescriptor | null>(null);

  const properties = useConditionalFilter(catalog.properties, !candidate, name);

  const items = useMemo(() => {
    let sliced: Array<PropertyDescriptor> = [];

    if (candidate) {
      const possibleNameSchema = candidate.schema.propertyNames;
      const possibleValueSchema = candidate.schema.additionalProperties || { type: 'string' };
      const possibleDescriptors: Array<PropertyDescriptor> = [];

      if (possibleNameSchema) {
        if (possibleNameSchema.examples) {
          possibleNameSchema.examples.forEach(it => {
            possibleDescriptors.push({
              ...candidate,
              name: candidate.name + '.' + it.toLowerCase(),
              schema: possibleValueSchema,
            });
          });
        }

        if (possibleNameSchema.enum) {
          possibleNameSchema.enum.forEach(it => {
            possibleDescriptors.push({
              ...candidate,
              name: candidate.name + '.' + it.toLowerCase(),
              schema: possibleValueSchema,
            });
          });
        }

        sliced = possibleDescriptors.filter(it => it.name.includes(name.toLowerCase()));
      }

      if (candidate.schema.type === 'object' && typeof candidate.schema.properties === 'object') {
        const candidateProperties = candidate.schema.properties;

        Object.keys(candidateProperties).forEach(it => sliced.push({
          ...candidate,
          name: candidate.name + '.' + it,
          schema: candidateProperties[it],
        }));
      }

      // if the possible value schema is an object type, we need to check if the possible name
      // contains the key of the object so that we can add the object properties to the list.
      //
      // For instance, given the following JSON Schema for a property with name `acme.property`:
      // {
      //   "type": "object",
      //   "additionalProperties": {
      //     "type": "object",
      //     "properties": { "foo": { "type": "string" }, "bar": { "type": "string" } }
      //   },
      //   "propertyNames": { "type": "string" }
      // }
      if (possibleValueSchema.type === 'object' && typeof possibleValueSchema.properties === 'object') {
        // check the input name value for a possible key of the object that would contain the value.
        // For instance, if the name is `acme.property.key`, we need to provide the hints for `foo` and `bar`
        // properties. The matching property would either be `acme.property.key.foo` or `acme.property.key.bar`.
        const potentialObjectKeys = name.substring(candidate.name.length + 1)
          .split(/\s*\.\s*/)
          .filter(Boolean);

        if (potentialObjectKeys.length === 1) {
          const candidateProperties = possibleValueSchema.properties;

          Object.keys(candidateProperties).forEach(it => sliced.push({
            ...candidate,
            name: candidate.name + '.' + potentialObjectKeys[0] + '.' + it,
            schema: candidateProperties[it],
          }));
        }
      }
    } else {
      sliced = properties.slice(0, 20);
    }

    // no matching descriptor found, try to add the current property name to the list
    // to allow the user to add this unknown property to the changeset with the
    // default JSON Schema set to `string`.
    if (sliced.length === 0 && name.length > 0) {
      sliced.push({
        name: name.trim(),
        typeName: candidate?.typeName || 'java.lang.String',
        schema: candidate?.schema.additionalProperties || { type: 'string' },
        description: candidate?.description,
        deprecation: candidate?.deprecation,
        defaultValue: candidate?.defaultValue,
      });
    }

    return sliced;
  }, [candidate, name, properties]);

  const onSearch = useCallback((query: string, event: ComboboxRoot.ChangeEventDetails) => {
    // ignore none event reasons and when they are carrying the empty query string.
    if (event.reason === 'none' && query.length === 0) {
      return;
    }

    // remove the current candidate if the input name query is not matching the current selection name.
    // For instance, if the user selected a `logger.level` configuration property and entered the
    // `json object` search phase, he needs to be able to leave this phase. By changing the search
    // query `logger.leve`, the user should remove the selection for this object schema property
    // and switch back to regular property search.
    if (query.length > 0 && candidate && !query.startsWith(candidate.name)) {
      onCandidateChange(null);
    }

    onNameChange(query);
  }, [candidate]);

  const onSelect = useCallback((property: PropertyDescriptor | null) => {
    if (property == null || property.schema.type !== 'object') {
      onChange(property);
    } else {
      onChange(null);
    }

    onCandidateChange(property);
  }, [onChange]);

  const onKeyDown = useCallback((event: KeyboardEvent<HTMLInputElement>) => {
    if (selection && event.key === 'Tab') {
      event.preventDefault();
      event.stopPropagation();
      onSelect(selection);
    }
  }, [selection]);

  return (
    <Combobox
      autoHighlight
      value={candidate}
      filter={null}
      items={items}
      itemToStringLabel={(it: PropertyDescriptor) => it.name}
      itemToStringValue={(it: PropertyDescriptor) => it.name}
      isItemEqualToValue={(a, b) => a.name === b.name}
      onValueChange={onSelect}
      onInputValueChange={onSearch}
      onItemHighlighted={(it: PropertyDescriptor | undefined) => onSelectionChange(it ?? null)}
    >
      <ComboboxInput
        id="vault-autocomplete-property-input"
        autoComplete="off"
        spellCheck={false}
        placeholder="e.g. spring.datasource.url"
        value={name}
        onKeyDown={onKeyDown}
        className="font-mono"
      />

      <ComboboxContent>
        <ComboboxList>
          {items.map(property => (
            <PropertyDescriptorItem key={property.name} property={property} />
          ))}
        </ComboboxList>
        <footer className="flex items-center justify-between border-t text-xs text-muted-foreground px-3 py-2">
          {candidate ? (
            <FormattedMessage
              tagName="span"
              defaultMessage="Dynamic configuration property selected"
              description="Combobox footer label when a dynamic, or complex object, configuration property is selected."
            />
          ) : (
            <FormattedMessage
              tagName="span"
              defaultMessage="{count, plural, one {1 match} other {{count, number} matches}}"
              description="Combobox footer label for the number of matches in the combobox."
              values={{ count: properties.length }}
            />
          )}
          <KbdGroup>
            <span className="flex items-center gap-1">
              <Kbd>↑↓</Kbd>
              <FormattedMessage
                defaultMessage="navigate"
                description="Combobox footer label for the up and down arrow keys to navigate the combobox items."
              />
            </span>
            <span className="flex items-center gap-1">
              <Kbd>Tab</Kbd>
              <FormattedMessage
                defaultMessage="complete"
                description="Combobox footer label for the tab key to autocomplete the current combobox item."
              />
            </span>
            <span className="flex items-center gap-1">
              <Kbd>Enter</Kbd>
              <FormattedMessage
                defaultMessage="select"
                description="Combobox footer label for the enter key to select the current combobox item."
              />
            </span>
          </KbdGroup>
        </footer>
      </ComboboxContent>
    </Combobox>
  );
}

export function PropertyValueInput<T>({ property, value, onChange, onSubmit }: {
  property: PropertyDescriptor,
  value: ConfigurationPropertyValue<T> | null
  onChange: (value: ConfigurationPropertyValue<T> | null) => void,
  onSubmit: () => void,
}) {
  const ref = useRef<HTMLInputElement>(null);

  const onKeyDown = useCallback((event: KeyboardEvent<HTMLElement>) => {
    if (value && event.key === 'Enter') {
      onSubmit();
    }
  }, [onSubmit]);

  useEffect(() => {
    if (ref.current) {
      ref.current.focus();
    }
  }, [property, ref.current]);

  return (
    <InputField
      id={property.name}
      ref={ref}
      property={property}
      value={value || undefined}
      onChange={onChange}
      onKeyDown={onKeyDown}
    />
  );
}

export function PropertyDialog<T>({ changeset, catalog, onAdd }: {
  changeset: ChangesetState,
  catalog: ServiceCatalog,
  onAdd: (property: ConfigurationProperty<T>) => void | Promise<unknown>,
}) {
  const [open, onOpenChange] = useState(false);
  const [value, onValueChange] = useState<ConfigurationPropertyValue<any> | null>(null);
  const [descriptor, onDescriptorChange] = useState<PropertyDescriptor | null>(null);

  const canAdd = changeset.properties
    .filter(it => it.name === descriptor?.name)
    .length === 0;

  const handleDescriptorChange = useCallback((selected: PropertyDescriptor | null) => {
    if (selected == null) {
      onDescriptorChange(null);
      onValueChange(null);
      return;
    }

    startTransition(() => {
      const encoded = resolveDefaultPropertyValue(selected);
      const decoded = useJsonSchemeTransform(selected.schema).decode(encoded);

      onDescriptorChange(selected);
      onValueChange({ encoded, decoded });
    });
  }, [onDescriptorChange, onValueChange]);

  const handleOpenChange = useCallback((state: boolean) => {
    if (state) {
      onOpenChange(true);
    } else {
      onOpenChange(false);
      onDescriptorChange(null);
      onValueChange(null);
    }
  }, [onOpenChange, onDescriptorChange, onValueChange]);

  const handleSubmit = useCallback(() => {
    if (canAdd && descriptor?.name && value) {
      onAdd({
        ...descriptor,
        value: value,
        state: ConfigurationPropertyState.ADDED,
      });

      handleOpenChange(false);
    }
  }, [canAdd, descriptor, value, handleOpenChange]);

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger
        render={
          <Button variant="outline">
            <PlusIcon />
            <AddPropertyLabel />
          </Button>
        }
      />
      <DialogContent className="sm:max-w-140">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-lg">
            <ListPlusIcon size="1rem" />
            <FormattedMessage
              defaultMessage="Add configuration property"
              description="Label for the dialog that allows adding a new configuration property"
            />
          </DialogTitle>
          <DialogDescription>
            <FormattedMessage
              defaultMessage="Select a configuration option to add it to the changeset."
              description="Description for the dialog that allows adding a new configuration property"
            />
          </DialogDescription>
        </DialogHeader>

        <FieldGroup>
          <Field>
            <FieldLabel htmlFor="vault-autocomplete-property-input">
              <PropertyNameLabel />
            </FieldLabel>
            <PropertyDescriptorInput
              catalog={catalog}
              onChange={handleDescriptorChange}
            />
            {!canAdd && (
              <FieldError className="text-xs">
                <FormattedMessage
                  defaultMessage="Property is already added to your profile. Please pick another one."
                  description="Warning message when adding a new configuration property to a profile that is already part of the profile changeset state."
                />
              </FieldError>
            )}
          </Field>

          <PropertyDeprecationAlert deprecation={descriptor?.deprecation} />

          {descriptor && canAdd && (
            <Field>
              <Label htmlFor={descriptor.name}>
                <PropertyValueLabel />
              </Label>
              <PropertyValueInput
                property={descriptor}
                value={value}
                onChange={onValueChange}
                onSubmit={handleSubmit}
              />
              <FieldDescription className="text-xs">
                <PropertyDescription value={descriptor.description} />

                {descriptor.schema.format && (
                  <span className="block mt-1">
                    <span className="text-muted-foreground mr-1">Format:</span>
                    <code className="font-medium text-foreground">{descriptor.schema.format}</code>
                  </span>
                )}
              </FieldDescription>
            </Field>
          )}
        </FieldGroup>

        <DialogFooter showCloseButton={true}>
          <Button
            disabled={!canAdd}
            onClick={handleSubmit}
          >
            <PlusIcon />
            <AddPropertyLabel />
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
