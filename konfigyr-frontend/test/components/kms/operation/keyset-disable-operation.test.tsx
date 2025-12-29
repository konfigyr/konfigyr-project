import { afterAll, afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { KeysetOperationDialog } from '@konfigyr/components/kms/operation/keyset-operation-dialog';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | operation | <KeysetDisableOperation/>', () => {
  const onCancel = vi.fn();

  const result = renderWithQueryClient((
    <>
      <KeysetOperationDialog
        namespace={namespaces.konfigyr}
        keyset={kms.signingKeyset}
        operation="disable"
        onClose={onCancel}
      />
      <Toaster />
    </>
  ));

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render keyset disable confirmation dialog', () => {
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('dialog')).toHaveAccessibleName('Are you sure you want to disable this keyset?');
    expect(result.getByRole('dialog')).toHaveAccessibleDescription(
      'This operation would prevent any cryptographic operations using the keyset. The cryptographic material would not be removed from the system and can be re-enabled at any time.',
    );
    expect(result.getByRole('button', { name: 'I understand the risks' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  // test('should successfully disable keyset', async () => {
  //   await userEvents.click(
  //     result.getByRole('button', { name: 'I understand the risks' }),
  //   );
  //
  //   await waitFor(() => {
  //     expect(result.getByRole('alert')).toBeInTheDocument();
  //     expect(result.getByRole('alert')).toHaveTextContent('Keyset has been successfully disabled');
  //   });
  // });
});
