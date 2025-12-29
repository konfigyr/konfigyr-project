import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeysetOperationDialog } from '@konfigyr/components/kms/operation/keyset-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeysetDecryptOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeysetOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.encryptingKeyset}
        operation='decrypt'
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset decrypt operation dialog and form', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Decrypt ciphertext');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'The operation will return the original plaintext that was encrypted using the same keyset.',
    );
    expect(result.getByRole('textbox', { name: 'Text to decrypt' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Additional authenticated data' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Decrypt ciphertext' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should validate form', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Decrypt ciphertext' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Text to decrypt' })).toBeInvalid();
      expect(result.getByRole('textbox', { name: 'Additional authenticated data' })).toBeValid();
    });
  });

  test('should fail to decrypt cipher text', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to decrypt' }),
      'invalid-cipher',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Decrypt ciphertext' }),
    );

    await waitFor(() => {
      expect(result.getByText('Can not perform decryption')).toBeInTheDocument();
    });
  });

  test('should successfully decrypt cipher text', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Text to decrypt' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to decrypt' }),
      'AVr7U4_7PUEu2qf7SYLUW4H2PZvJIcRQvLlCVYpWmKxzHD8rnUx2sgxQ',
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Additional authenticated data' }),
      'authenticated-data',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Decrypt ciphertext' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Decrypted text' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Decrypted text' })).toHaveValue('decrypted data');
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
