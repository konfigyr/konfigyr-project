import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeysetOperationDialog } from '@konfigyr/components/kms/operation/keyset-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeysetDestroyOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeysetOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        operation="destroy"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset destroy confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Are you sure you want to destroy this keyset?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'Destroying this keyset will permanently wipe all cryptographic material from the system. Any data encrypted with these keys will become irrecoverable.',
    );
    expect(result.getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should successfully destroy keyset', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Keyset has been successfully destroyed');
    });
  });
});
