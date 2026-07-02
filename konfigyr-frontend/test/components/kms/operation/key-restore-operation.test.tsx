import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeyOperationDialog } from '@konfigyr/components/kms/operation/key-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeyRestoreOperation/>', () => {
  const onCancel = vi.fn();
  const user = userEvents.setup();

  const result = renderWithQueryClient((
    <>
      <KeyOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.destroyedKeyset}
        value={kms.destroyedKeyset.keys[0]}
        operation="restore"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render restore key confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Restore key?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'This will cancel the scheduled destruction and move the key back to a disabled state. No data will be lost. The key will not resume operations until it is explicitly reactivated.',
    );
    expect(result.getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should successfully restore key from scheduled for destruction', async () => {
    await user.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Key restored. Destruction was cancelled and the key is now disabled and will not be used for any operations until reactivated.');
    });
  });
});
