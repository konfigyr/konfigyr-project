import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeyOperationDialog } from '@konfigyr/components/kms/operation/key-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeyDisableOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeyOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        value={kms.signingKeyset.keys[0]}
        operation="deactivate"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render key deactivate confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Deactivate this key?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'This key will no longer be used for new operations. Existing data it protected will remain readable, but you will need to reactivate it before it can encrypt or sign anything new.',
    );
    expect(result.getByRole('button', { name: 'I understand the risks' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should successfully deactivate keyset', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'I understand the risks' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Key has been successfully disabled');
    });
  });
});
