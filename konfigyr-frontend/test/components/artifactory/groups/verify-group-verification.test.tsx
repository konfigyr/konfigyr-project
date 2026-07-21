import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { VerifyGroupVerificationButton } from '@konfigyr/components/artifactory/groups/verify-group-verification';
import {
  ClaimVerifiedSuccessMessage,
  ClaimVerifiedWarningMessage,
} from '@konfigyr/components/artifactory/groups/messages';

import type { GroupVerification } from '@konfigyr/hooks/types';

const toast = vi.hoisted(() => ({
  success: vi.fn(),
  warning: vi.fn(),
}));

const verifyGroupVerification = vi.hoisted(() => vi.fn());
const errorNotification = vi.hoisted(() => vi.fn());

vi.mock('sonner', () => ({ toast }));
vi.mock('@konfigyr/hooks', () => ({
  useVerifyGroupVerification: () => ({
    isPending: false,
    mutateAsync: verifyGroupVerification,
  }),
}));
vi.mock('@konfigyr/components/error', () => ({
  useErrorNotification: () => errorNotification,
}));

describe('components | groups | <VerifyGroupVerificationButton/>', () => {
  const verification: GroupVerification = {
    id: 'verification-1',
    groupId: 'com.example.group',
    state: 'PENDING',
    createdAt: '2026-07-07T00:00:00Z',
    verifiedAt: null,
    revokedAt: null,
  };

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('opens the verification confirmation dialog', async () => {
    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <VerifyGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Verify verification</button>
      </VerifyGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Verify verification' }));

    const dialog = await waitFor(() => getByRole('alertdialog'));

    expect(dialog).toHaveAccessibleName('Verify group verification claim');
    expect(dialog).toHaveAccessibleDescription(
      'Are you sure you want to verify the group verification claim for com.example.group?',
    );

    expect(getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Verify claim' })).toBeInTheDocument();
  });

  test('submits verification successfully and closes the dialog', async () => {
    verifyGroupVerification.mockResolvedValueOnce({
      ...verification,
      state: 'ACTIVE',
    });

    const user = userEvents.setup();

    const { getByRole, queryByRole } = renderWithQueryClient((
      <VerifyGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Verify verification</button>
      </VerifyGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Verify verification' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Verify claim' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(verifyGroupVerification).toHaveBeenCalledExactlyOnceWith(verification.groupId);
      expect(toast.success).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({
          type: ClaimVerifiedSuccessMessage,
        }),
      );
      expect(queryByRole('alertdialog')).not.toBeInTheDocument();
    });
  });

  test('shows a warning when verification does not become active', async () => {
    verifyGroupVerification.mockResolvedValueOnce({
      ...verification,
      state: 'FAILED',
    });

    const user = userEvents.setup();

    const { getByRole, queryByRole } = renderWithQueryClient((
      <VerifyGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Verify verification</button>
      </VerifyGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Verify verification' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Verify claim' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(verifyGroupVerification).toHaveBeenCalledExactlyOnceWith(verification.groupId);
      expect(toast.warning).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({
          type: ClaimVerifiedWarningMessage,
        }),
      );
      expect(queryByRole('alertdialog')).not.toBeInTheDocument();
    });
  });

  test('reports errors from the verification request', async () => {
    const error = new Error('Failed to verify group verification');
    verifyGroupVerification.mockRejectedValueOnce(error);

    const user = userEvents.setup();

    const { getByRole, queryByRole } = renderWithQueryClient((
      <VerifyGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Verify verification</button>
      </VerifyGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Verify verification' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Verify claim' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(verifyGroupVerification).toHaveBeenCalledExactlyOnceWith(verification.groupId);
      expect(errorNotification).toHaveBeenCalledExactlyOnceWith(error);
      expect(toast.success).not.toHaveBeenCalled();
      expect(toast.warning).not.toHaveBeenCalled();
      expect(queryByRole('alertdialog')).not.toBeInTheDocument();
    });
  });
});
