import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { GroupVerificationForm } from '@konfigyr/components/artifactory/groups/group-verification-form';

describe('components | groups | <GroupVerificationForm/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render the claim form', async () => {
    const { getByRole } = renderWithQueryClient(
      <GroupVerificationForm
        defaultValues={{ groupId: '', verificationMethod: 'DNS' }}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Group Id' })).toBeInTheDocument();
      expect(getByRole('radio', { name: /dns/i })).toBeChecked();
      expect(getByRole('radio', { name: /source code/i })).toBeInTheDocument();
    });
  });

  test('should render a prefilled and disabled groupId field for an existing claim', async () => {
    const { getByRole } = renderWithQueryClient(
      <GroupVerificationForm
        defaultValues={{ groupId: 'io.github.acme', verificationMethod: 'SOURCE_CODE' }}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Group Id' })).toHaveValue('io.github.acme');
      expect(getByRole('textbox', { name: 'Group Id' })).toBeDisabled();
      expect(getByRole('radio', { name: /source code/i })).toBeChecked();
    });
  });
});
