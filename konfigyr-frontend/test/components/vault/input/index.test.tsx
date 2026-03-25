import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { InputField } from '@konfigyr/components/vault/input';

import type {
  ConfigurationPropertyValue,
  PropertyDescriptor,
  PropertyJsonSchema,
} from '@konfigyr/hooks/types';

const propertyFor = (schema: PropertyJsonSchema): PropertyDescriptor => ({
  name: 'test.property',
  typeName: 'java.lang.String',
  description: 'Test property description',
  schema,
});

const valueFor = (value: string): ConfigurationPropertyValue<string> => ({
  encoded: value, decoded: value,
});

describe('components | vault | properties | <InputField/>', () => {
  afterEach(() => cleanup());

  test('should render input field for a simple string JSON schema property type', () => {
    const property = propertyFor({ type: 'string' });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={valueFor('existing value')} />,
    );

    const field = getByRole('textbox', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue('existing value');
    expect(field).toHaveAccessibleDescription(property.description);
    expect(field).not.toHaveAttribute('list');
  });

  test('should render input field for a date JSON schema property type', () => {
    const property = propertyFor({ type: 'string', format: 'date' });

    const { getByLabelText } = renderWithMessageProvider(
      <InputField property={property} />,
    );

    const field = getByLabelText(property.name);
    expect(field).toBeInTheDocument();
    expect(field).toHaveAccessibleDescription(property.description);
    expect(field).toHaveAttribute('type', 'date');
  });

  test('should render input field for a time JSON schema property type', () => {
    const property = propertyFor({ type: 'string', format: 'time' });

    const { getByLabelText } = renderWithMessageProvider(
      <InputField property={property} />,
    );

    const field = getByLabelText(property.name);
    expect(field).toBeInTheDocument();
    expect(field).toHaveAccessibleDescription(property.description);
    expect(field).toHaveAttribute('type', 'time');
  });

  test('should render input field for a date time JSON schema property type', () => {
    const date = new Date();
    const property = propertyFor({ type: 'string', format: 'date-time' });
    const value = { encoded: date.toISOString(), decoded: date };

    const { getByLabelText } = renderWithMessageProvider(
      <InputField property={property} value={value} />,
    );

    const field = getByLabelText(property.name);
    expect(field).toBeInTheDocument();
    expect(field).toHaveAccessibleDescription(property.description);
    expect(field).toHaveAttribute('type', 'datetime-local');
  });

  test('should validate text input field based on JSON schema validation min length constraint', () => {
    const property = propertyFor({ type: 'string', minLength: 10, maxLength: 20 });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={valueFor('validated text')} />,
    );

    const field = getByRole('textbox', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue('validated text');
    expect(field).toHaveAttribute('minlength', '10');
    expect(field).toHaveAttribute('maxlength', '20');
  });

  test('should render input field for a string JSON schema property type with examples', () => {
    const property = propertyFor({
      type: 'string',
      examples: ['foo', 'bar', 'baz'],
    });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={valueFor('foo')} />,
    );

    const field = getByRole('combobox', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue('foo');
    expect(field).toHaveAttribute('list');

    const hints = field.nextSibling;
    expect(hints).toBeInTheDocument();
    expect(hints).not.toBeVisible();
    expect(hints).toHaveRole('listbox');

    const options = hints!.childNodes;
    expect(options).toHaveLength(3);
    expect(options[0]).toHaveValue('foo');
    expect(options[1]).toHaveValue('bar');
    expect(options[2]).toHaveValue('baz');
  });

  test('should render input field for a string JSON schema property with charset format', () => {
    const property = propertyFor({ type: 'string', format: 'charset' });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={valueFor('UTF-8')} />,
    );

    const field = getByRole('combobox', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue('UTF-8');
    expect(field).toHaveAttribute('list');

    const hints = field.nextSibling;
    expect(hints).toHaveRole('listbox');

    const options = hints!.childNodes;
    expect(options).toHaveLength(5);
    expect(options[0]).toHaveValue('UTF-8');
    expect(options[1]).toHaveValue('UTF-16');
  });

  test('should render input field for a string JSON schema property with language format', () => {
    const property = propertyFor({ type: 'string', format: 'language' });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={valueFor('en')} />,
    );

    const field = getByRole('combobox', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue('en');
    expect(field).toHaveAttribute('list');

    const hints = field.nextSibling;
    expect(hints).toHaveRole('listbox');

    const options = hints!.childNodes;
    expect(options).toHaveLength(10);
    expect(options[0]).toHaveValue('en-US');
    expect(options[0]).toHaveTextContent('en-US - English, United States');
    expect(options[1]).toHaveValue('en-GB');
    expect(options[1]).toHaveTextContent('en-GB - English, United Kingdom');
  });

  test('should render input field for a string JSON schema property with mime-type format', () => {
    const property = propertyFor({ type: 'string', format: 'mime-type' });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={valueFor('text/plain')} />,
    );

    const field = getByRole('combobox', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue('text/plain');
    expect(field).toHaveAttribute('list');

    const hints = field.nextSibling;
    expect(hints).toHaveRole('listbox');

    const options = hints!.childNodes;
    expect(options).toHaveLength(10);
    expect(options[0]).toHaveValue('text/html');
    expect(options[1]).toHaveValue('text/plain');
  });

  test('should render input field for a string JSON schema property with time-zone format', () => {
    const property = propertyFor({ type: 'string', format: 'time-zone' });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={valueFor('UTC')} />,
    );

    const field = getByRole('combobox', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue('UTC');
    expect(field).toHaveAttribute('list');

    const hints = field.nextSibling;
    expect(hints).toHaveRole('listbox');

    const options = hints!.childNodes;
    expect(options).toHaveLength(9);
    expect(options[0]).toHaveValue('UTC');
    expect(options[0]).toHaveTextContent('UTC');
    expect(options[1]).toHaveValue('America/New_York');
    expect(options[1]).toHaveTextContent('America/New York - Eastern Time');
  });

  test('should render input field for a boolean JSON schema property type', async () => {
    const property = propertyFor({ type: 'boolean' });
    const value = { encoded: 'true', decoded: true };
    const onChange = vi.fn();

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={value} onChange={onChange} />,
    );

    const field = getByRole('switch', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toBeChecked();
    expect(field).toHaveAccessibleDescription(property.description);

    await userEvent.click(field);

    await waitFor(() => expect(onChange).toBeCalledWith({ encoded: 'false', decoded: false }));
  });

  test('should render input field for an integer JSON schema property type', async () => {
    const property = propertyFor({ type: 'integer' });
    const value = { encoded: '148', decoded: 148 };
    const onChange = vi.fn();

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={value} onChange={onChange} />,
    );

    const field = getByRole('spinbutton', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue(148);
    expect(field).toHaveAccessibleDescription(property.description);

    await userEvent.type(field, '7');

    await waitFor(() => expect(onChange).toBeCalledWith({ encoded: '1487', decoded: 1487 }));
  });

  test('should render input field for a number JSON schema property type', async () => {
    const property = propertyFor({ type: 'number' });
    const value = { encoded: '10.25', decoded: 10.25 };
    const onChange = vi.fn();

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={value} onChange={onChange} />,
    );

    const field = getByRole('spinbutton', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue(10.25);
    expect(field).toHaveAccessibleDescription(property.description);
    expect(field).toBeValid();

    await userEvent.type(field, '2');

    await waitFor(() => expect(onChange).toBeCalledWith({ encoded: '10.252', decoded: 10.252 }));
  });

  test('should validate numeric input field based on JSON schema validation min constraint', () => {
    const property = propertyFor({
      type: 'integer',
      minimum: 10,
      maximum: 20,
    });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={{ encoded: '2', decoded: 2 }} />,
    );

    const field = getByRole('spinbutton', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue(2);
    expect(field).toBeInvalid();
  });

  test('should validate numeric input field based on JSON schema validation max constraint', () => {
    const property = propertyFor({
      type: 'integer',
      minimum: 10,
      maximum: 20,
    });

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={{ encoded: '22', decoded: 22 }} />,
    );

    const field = getByRole('spinbutton', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toBeInvalid();
  });

  test('should render input field for a duration JSON schema property type', async () => {
    const property = propertyFor({ type: 'string', format: 'duration' });
    const value = { encoded: '21s', decoded: { value: 21, unit: 's' } };
    const onChange = vi.fn();

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={value} onChange={onChange} />,
    );

    const field = getByRole('spinbutton', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue(21);

    await userEvent.type(field, '0');
    await waitFor(() => expect(onChange).toBeCalledWith({
      encoded: '210s', decoded: { value: 210, unit: 's' },
    }));

    const trigger = getByRole('button', { name: 's' });
    expect(trigger).toBeInTheDocument();

    await userEvent.click(trigger);
    await waitFor(() => {
      expect(getByRole('menuitemradio', { name: 'Milliseconds' } )).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'Seconds' } )).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'Minutes' } )).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'Hours' } )).toBeInTheDocument();
      expect(getByRole('menuitemradio', { name: 'Days' } )).toBeInTheDocument();
    });

    await userEvent.click(getByRole('menuitemradio', { name: 'Days' } ));
    await waitFor(() => expect(onChange).toBeCalledWith({
      encoded: '21d', decoded: { value: 21, unit: 'd' },
    }));
  });

  test('should render input field for a data size JSON schema property type', async () => {
    const property = propertyFor({ type: 'string', format: 'data-size' });
    const value = { encoded: '128MB', decoded: { value: 128, unit: 'MB' } };
    const onChange = vi.fn();

    const { getByRole } = renderWithMessageProvider(
      <InputField property={property} value={value} onChange={onChange} />,
    );

    const field = getByRole('spinbutton', { name: property.name });
    expect(field).toBeInTheDocument();
    expect(field).toHaveValue(128);

    await userEvent.type(field, '0');

    expect(onChange).toBeCalledWith({
      encoded: '1280MB', decoded: { value: 1280, unit: 'MB' },
    });

    const trigger = getByRole('button', { name: 'MB' });
    expect(trigger).toBeInTheDocument();

    await userEvent.click(trigger);
    expect(getByRole('menuitemradio', { name: 'Bytes' } )).toBeInTheDocument();
    expect(getByRole('menuitemradio', { name: 'Kilobytes' } )).toBeInTheDocument();
    expect(getByRole('menuitemradio', { name: 'Megabytes' } )).toBeInTheDocument();
    expect(getByRole('menuitemradio', { name: 'Gigabytes' } )).toBeInTheDocument();
    expect(getByRole('menuitemradio', { name: 'Terabytes' } )).toBeInTheDocument();

    await userEvent.click(getByRole('menuitemradio', { name: 'Bytes' } ));
    expect(onChange).toBeCalledWith({
      encoded: '128B', decoded: { value: 128, unit: 'B' },
    });
  });

});
