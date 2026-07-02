import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { CreateKeysetForm } from '@konfigyr/components/kms/keyset-create';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | <CreateKeysetForm/>', () => {
  const onCreate = vi.fn();
  const user = userEvents.setup();

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
    await user.type(
      result.getByRole('textbox', { name: 'Keyset name' }),
      'Keyset name that is too long and should trigger validation error',
    );

    await user.click(
      result.getByRole('button', { name: 'Create keyset' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Keyset name' })).toBeInvalid();
      expect(result.getByRole('combobox')).toBeInvalid();
    });
  });

  test('should attempt to create a keyset that already exists', async () => {
    await user.clear(
      result.getByRole('textbox', { name: 'Keyset name' }),
    );

    await user.type(
      result.getByRole('textbox', { name: 'Keyset name' }),
      'existing-keyset',
    );

    await user.click(
      result.getByRole('combobox', { name: 'Keyset algorithm' }),
    );

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(5);
    });

    await user.click(
      result.getByRole('option', { name: /AES128-GCM/ }),
    );

    await user.click(
      result.getByRole('button', { name: 'Create keyset' }),
    );

    await waitFor(() => {
      expect(result.getByText('Keyset already exists.')).toBeInTheDocument();
    });
  });

  test('should create a new keyset for namespace', async () => {
    await user.clear(
      result.getByRole('textbox', { name: 'Keyset name' }),
    );

    await user.type(
      result.getByRole('textbox', { name: 'Keyset name' }),
      'new-keyset',
    );

    await user.click(
      result.getByRole('combobox', { name: 'Keyset algorithm' }),
    );

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(5);
    });

    await user.click(
      result.getByRole('option', { name: /AES256-GCM/ }),
    );

    await user.click(
      result.getByRole('button', { name: 'Create keyset' }),
    );

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledOnce();
    });
  });
});
