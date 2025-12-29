import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeysetOperationDialog } from '@konfigyr/components/kms/operation/keyset-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeysetSigningOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeysetOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        operation="sign"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset signing operation form', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Create signature');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'This operation will generate a Base64URL encoded signature.',
    );
    expect(result.getByRole('textbox', { name: 'Text to sign' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Create signature' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should validate form', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Create signature' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Text to sign' })).toBeInvalid();
    });
  });

  test('should fail to sign plain text', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to sign' }),
      'invalid-plaintext',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Create signature' }),
    );

    await waitFor(() => {
      expect(result.getByText('Can not generate signature')).toBeInTheDocument();
    });
  });

  test('should successfully sign plain text', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Text to sign' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Text to sign' }),
      'Text to sign',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Create signature' }),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Signature' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Signature' })).toHaveValue('signed-data');
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
