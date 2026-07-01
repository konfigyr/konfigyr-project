import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeyOperationDialog } from '@konfigyr/components/kms/operation/key-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeyReactivateOperation/>', () => {
  const onCancel = vi.fn();
  const user = userEvents.setup();

  const result = renderWithQueryClient((
    <>
      <KeyOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        value={kms.signingKeyset.keys[0]}
        operation="reactivate"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render reactivate key confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Restore key?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'This key will resume being used for new cryptographic operations. Make sure it has not been compromised before reactivating. If you have any doubt, rotate the keyset instead.',
    );
    expect(result.getByRole('button', { name: 'Reactivate' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should successfully reactivate key', async () => {
    await user.click(
      result.getByRole('button', { name: 'Reactivate' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Key has been successfully reactivated');
    });
  });
});
