import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeysetOperationDialog } from '@konfigyr/components/kms/operation/keyset-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeysetVerifySignatureOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeysetOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        operation="verify"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset signature verification operation form', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Verify signature');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'Verify the signature of data using the selected keyset. The operation will return true if the signature is valid, false otherwise.',
    );
    expect(result.getByRole('textbox', { name: 'Signature to verify' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Text to verify' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Verify signature' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should validate form', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Verify signature' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Signature to verify' })).toBeInvalid();
      expect(result.getByRole('textbox', { name: 'Text to verify' })).toBeInvalid();
    });
  });

  test('should fail to validate signature due to server error', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Signature to verify' }),
      'some-signature',
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to verify' }),
      'invalid-plaintext',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Verify signature' }),
    );

    await waitFor(() => {
      expect(result.getByText('Can not verify signature')).toBeInTheDocument();
    });
  });

  test('should fail to validate signature', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Text to verify' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to verify' }),
      'Plaintext to verify',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Verify signature' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Signature is invalid');
    });
  });

  test('should successfully validate signature', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Signature to verify' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Signature to verify' }),
      'valid-signature',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Verify signature' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Signature is valid');
    });
  });

  test('should close the keyset operation dialog', async () => {
    await userEvents.click(result.getByRole('button', { name: 'Cancel' }));

    await waitFor(() => {
      expect(result.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });
});
