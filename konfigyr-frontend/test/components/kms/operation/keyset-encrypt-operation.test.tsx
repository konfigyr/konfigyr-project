import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeysetOperationDialog } from '@konfigyr/components/kms/operation/keyset-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeysetEncryptOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeysetOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.encryptingKeyset}
        operation="encrypt"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset encrypt operation form', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Encrypt data');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'The operation will return a ciphertext that is Base64URL encoded and can be decrypted using the same keyset.',
    );
    expect(result.getByRole('textbox', { name: 'Text to encrypt' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Additional authenticated data' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Encrypt data' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should validate form', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Encrypt data' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Text to encrypt' })).toBeInvalid();
      expect(result.getByRole('textbox', { name: 'Additional authenticated data' })).toBeValid();
    });
  });

  test('should fail to encrypt plain text', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to encrypt' }),
      'invalid-plaintext',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Encrypt data' }),
    );

    await waitFor(() => {
      expect(result.getByText('Can not perform encryption')).toBeInTheDocument();
    });
  });

  test('should successfully encrypt plain text', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Text to encrypt' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to encrypt' }),
      'text to encrypt',
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Additional authenticated data' }),
      'authenticated-data',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Encrypt data' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Encrypted text' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Encrypted text' })).toHaveValue('encrypted-data');
      expect(result.getByRole('button', { name: 'Close' })).toBeInTheDocument();
      expect(result.getByRole('button', { name: 'Copy' })).toBeInTheDocument();
    });
  });

  test('should close the keyset operation dialog', async () => {
    await userEvents.click(result.getByRole('button', { name: 'Close' }));

    await waitFor(() => {
      expect(result.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });
});
