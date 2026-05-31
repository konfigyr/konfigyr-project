import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeyOperationDialog } from '@konfigyr/components/kms/operation/key-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeyCompromisedOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeyOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        value={kms.signingKeyset.keys[0]}
        operation="compromise"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render compromise key confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Mark key as compromised?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'This immediately disables the key and flags it as untrusted. Any data it was used to protect should be considered at risk and re-encrypted with a new key. This action cannot be undone.',
    );
    expect(result.getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should successfully mark key as compromised', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Key has been mark as compromised.');
    });
  });
});
