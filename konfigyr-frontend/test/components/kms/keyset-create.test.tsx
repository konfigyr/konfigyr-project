import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { CreateKeysetForm } from '@konfigyr/components/kms/keyset-create';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | <CreateKeysetForm/>', () => {
  const onCreate = vi.fn();

  const result = renderWithQueryClient((
    <>
      <CreateKeysetForm namespace={namespaces.konfigyr} onCreate={onCreate}/>
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset create form', () => {
    expect(result.getByRole('textbox', { name: 'Keyset name' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Purpose' })).toBeInTheDocument();
    expect(result.getByRole('combobox')).toBeInTheDocument();
    expect(result.getByRole('button')).toBeInTheDocument();
  });

  test('should validate form', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Keyset name' }),
      'Keyset name that is too long and should trigger validation error',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Keyset name' })).toBeInvalid();
      expect(result.getByRole('combobox')).toBeInvalid();
    });
  });

  test('should attempt to create a keyset that already exists', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Keyset name' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Keyset name' }),
      'existing-keyset',
    );

    await userEvents.selectOptions(
      result.container.querySelector('select')!, 'AES128_GCM',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Keyset already exists.')).toBeInTheDocument();
    });
  });

  test('should create a new keyset for namespace', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Keyset name' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Keyset name' }),
      'new-keyset',
    );

    await userEvents.selectOptions(
      result.container.querySelector('select')!, 'AES256_GCM',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledOnce();
    });
  });
});
