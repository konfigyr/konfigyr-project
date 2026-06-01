import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeyOperationDialog } from '@konfigyr/components/kms/operation/key-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeyDestroyOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeyOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        value={kms.signingKeyset.keys[0]}
        operation="destroy"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render key destroy confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Are you sure you want to destroy this key?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'The key will enter a pending deletion period, after which its material will be permanently erased and all data it protected will become unreadable. Once destroyed, there is no recovery. Make sure all data this key protects has been re-encrypted before proceeding.',
    );
    expect(result.getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should successfully schedule key for destruction', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Key has been scheduled for destruction');
    });
  });
});
