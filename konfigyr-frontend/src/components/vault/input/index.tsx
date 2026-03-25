import { useCallback } from 'react';
import { useJsonSchemeTransform } from '@konfigyr/hooks/artifactory/hooks';
import {
  InlineEditContainer,
  useFocusEffect,
  useInlineEdit,
  useKeyboardEvents,
} from '@konfigyr/components/ui/inline-edit';

import { BooleanField } from './boolean';
import { EnumerationField } from './enumeration';
import { SchemaHints, useSchemaHints } from './hints';
import { DurationField } from './duration';
import { DataSizeField } from './data-size';
import { NumericField } from './numeric';
import { TextField } from './text';

import type { FunctionComponent } from 'react';
import type {
  ConfigurationPropertyValue,
  PropertyDescriptor,
  PropertyJsonSchema,
} from '@konfigyr/hooks/types';
import type { InputFieldProps } from './types';

export {
  type InputFieldProps,
  BooleanField,
  EnumerationField,
  DurationField,
  DataSizeField,
  NumericField,
  TextField,
};

const useSchemaInputComponent = (schema: PropertyJsonSchema): FunctionComponent<any> => {
  switch (schema.type) {
    case 'number':
    case 'integer': {
      return NumericField;
    }
    case 'boolean': {
      return BooleanField;
    }
    default: {
      if (schema.format === 'data-size') {
        return DataSizeField;
      }
      if (schema.format === 'duration') {
        return DurationField;
      }
      if (schema.enum && schema.enum.length > 0) {
        return EnumerationField;
      }
      return TextField;
    }
  }
};

export function InputField<T>({ property, ref, value, onChange, onBlur, onKeyDown }: InputFieldProps<HTMLElement, ConfigurationPropertyValue<T>>) {
  const InputComponent = useSchemaInputComponent(property.schema);
  const transform = useJsonSchemeTransform<T>(property.schema);
  const hints = useSchemaHints(property.schema);

  const onTransformChange = useCallback((decoded: T) => {
    const encoded = transform.encode(decoded);
    return onChange?.({ encoded, decoded });
  }, [transform, onChange]);

  return (
    <>
      <InputComponent
        ref={ref}
        list={hints?.id}
        property={property}
        value={value?.decoded}
        onChange={onTransformChange}
        onBlur={onBlur}
        onKeyDown={onKeyDown}
        aria-label={property.schema.title || property.name}
        aria-description={property.schema.description || property.description}
      />

      {hints && (
        <SchemaHints {...hints} />
      )}
    </>
  );
}

export function InlineInputField<T>({ property }: { property: PropertyDescriptor }) {
  const context = useInlineEdit<ConfigurationPropertyValue<T>>();
  const ref = useFocusEffect<any>(context);
  const onKeyDown = useKeyboardEvents(context);

  return (
    <InlineEditContainer>
      <InputField
        ref={ref}
        property={property}
        value={context.value}
        onChange={context.setValue}
        onBlur={context.onCancel}
        onKeyDown={onKeyDown}
      />
    </InlineEditContainer>
  );
}
