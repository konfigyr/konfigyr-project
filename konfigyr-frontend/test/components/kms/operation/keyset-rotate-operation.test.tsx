import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeysetOperationDialog } from '@konfigyr/components/kms/operation/keyset-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeysetRotateOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeysetOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        operation="rotate"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset rotation confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Generate a new primary key now?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'Generates a new primary key for future operations. The previous key is retained for decryption and signature verification.',
    );
    expect(result.getByRole('button', { name: 'Generate new key' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should successfully rotate keyset', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Generate new key' }),
    );

    await waitFor(() => {
      expect(result.getByRole('alert')).toBeInTheDocument();
      expect(result.getByRole('alert')).toHaveTextContent('Keyset has been successfully rotated');
    });
  });

  // test('should close the keyset operation dialog', async () => {
  //   await userEvents.click(result.getByRole('button', { name: 'Cancel' }));
  //
  //   await waitFor(() => {
  //     expect(result.queryByRole('dialog')).not.toBeInTheDocument();
  //   });
  // });
});
