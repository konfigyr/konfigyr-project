import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import {
  CancelGroupVerificationButton,
  RevokeGroupVerificationButton,
} from '@konfigyr/components/artifactory/groups/revoke-group-verification';
import {
  ClaimCanceledSuccessMessage,
  ClaimRevokedSuccessMessage,
} from '@konfigyr/components/artifactory/groups/messages';

import type { GroupVerification } from '@konfigyr/hooks/types';

const toast = vi.hoisted(() => ({
  success: vi.fn(),
}));

const revokeGroupVerification = vi.hoisted(() => vi.fn());
const errorNotification = vi.hoisted(() => vi.fn());

vi.mock('sonner', () => ({ toast }));
vi.mock('@konfigyr/hooks', () => ({
  useRevokeGroupVerification: () => ({
    isPending: false,
    mutateAsync: revokeGroupVerification,
  }),
}));

vi.mock('@konfigyr/components/error', () => ({
  useErrorNotification: () => errorNotification,
}));

describe('components | groups | <RevokeGroupVerificationButton/>', () => {
  const verification: GroupVerification = {
    id: 'verification-1',
    groupId: 'com.example.group',
    state: 'ACTIVE',
    createdAt: '2026-07-07T00:00:00Z',
    verifiedAt: '2026-07-07T00:00:00Z',
    revokedAt: null,
  };

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should open the revoke confirmation dialog', async () => {
    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <RevokeGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Revoke verification</button>
      </RevokeGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Revoke verification' }));

    const dialog = await waitFor(() => getByRole('alertdialog'));

    expect(dialog).toHaveAccessibleName('Revoke group verification claim');
    expect(dialog).toHaveAccessibleDescription(
      'Are you sure you want to revoke the group verification claim for com.example.group?',
    );

    expect(getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Revoke claim' })).toBeInTheDocument();
  });

  test('shoul submit the revoke action successfully', async () => {
    revokeGroupVerification.mockResolvedValueOnce(undefined);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <RevokeGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Revoke verification</button>
      </RevokeGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Revoke verification' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Revoke claim' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(revokeGroupVerification).toHaveBeenCalledExactlyOnceWith(verification.groupId);
      expect(toast.success).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({
          type: ClaimRevokedSuccessMessage,
          props: expect.objectContaining({
            groupId: verification.groupId,
          }),
        }),
      );
    });
  });

  test('should open the cancel confirmation dialog', async () => {
    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <CancelGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Cancel verification</button>
      </CancelGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Cancel verification' }));

    const dialog = await waitFor(() => getByRole('alertdialog'));

    expect(dialog).toHaveAccessibleName('Cancel group verification claim');
    expect(dialog).toHaveAccessibleDescription(
      'Are you sure you want to cancel the group verification claim for com.example.group?',
    );

    expect(getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Cancel claim' })).toBeInTheDocument();
  });

  test('should submit the cancel action successfully', async () => {
    revokeGroupVerification.mockResolvedValueOnce(undefined);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <CancelGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Cancel verification</button>
      </CancelGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Cancel verification' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Cancel claim' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(revokeGroupVerification).toHaveBeenCalledExactlyOnceWith(verification.groupId);
      expect(toast.success).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({
          type: ClaimCanceledSuccessMessage,
          props: expect.objectContaining({
            groupId: verification.groupId,
          }),
        }),
      );
    });
  });

  test('should report errors for the revoke action', async () => {
    const error = new Error('Failed to revoke group verification');
    revokeGroupVerification.mockRejectedValueOnce(error);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <RevokeGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Revoke verification</button>
      </RevokeGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Revoke verification' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Revoke claim' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(revokeGroupVerification).toHaveBeenCalledExactlyOnceWith(verification.groupId);
      expect(errorNotification).toHaveBeenCalledExactlyOnceWith(error);
    });

    expect(toast.success).not.toHaveBeenCalled();
  });

  test('should report errors for the cancel action', async () => {
    const error = new Error('Failed to revoke group verification');
    revokeGroupVerification.mockRejectedValueOnce(error);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <CancelGroupVerificationButton namespace="konfigyr" verification={verification}>
        <button type="button">Cancel verification</button>
      </CancelGroupVerificationButton>
    ));

    await user.click(getByRole('button', { name: 'Cancel verification' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Cancel claim' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(revokeGroupVerification).toHaveBeenCalledExactlyOnceWith(verification.groupId);
      expect(errorNotification).toHaveBeenCalledExactlyOnceWith(error);
    });

    expect(toast.success).not.toHaveBeenCalled();
  });
});
