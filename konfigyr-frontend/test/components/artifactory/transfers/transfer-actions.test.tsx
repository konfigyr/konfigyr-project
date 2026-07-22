import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import {
  AcceptTransferButton,
  CancelTransferButton,
  RejectTransferButton,
} from '@konfigyr/components/artifactory/transfers/transfer-actions';
import {
  TransferAcceptedSuccessMessage,
  TransferCanceledSuccessMessage,
  TransferRejectedSuccessMessage,
} from '@konfigyr/components/artifactory/transfers/messages';

import type { ArtifactOwnershipTransfer } from '@konfigyr/hooks/types';

const toast = vi.hoisted(() => ({ success: vi.fn() }));

const acceptTransfer = vi.hoisted(() => vi.fn());
const rejectTransfer = vi.hoisted(() => vi.fn());
const cancelTransfer = vi.hoisted(() => vi.fn());
const errorNotification = vi.hoisted(() => vi.fn());

vi.mock('sonner', () => ({ toast }));
vi.mock('@konfigyr/hooks', () => ({
  useAcceptTransfer: () => ({ isPending: false, mutateAsync: acceptTransfer }),
  useRejectTransfer: () => ({ isPending: false, mutateAsync: rejectTransfer }),
  useCancelTransfer: () => ({ isPending: false, mutateAsync: cancelTransfer }),
}));

vi.mock('@konfigyr/components/error', () => ({
  useErrorNotification: () => errorNotification,
}));

describe('components | transfers | <AcceptTransferButton/>', () => {
  const transfer: ArtifactOwnershipTransfer = {
    id: 'transfer-1',
    groupId: 'com.example.group',
    from: { id: '1', slug: 'konfigyr' },
    to: { id: '2', slug: 'ebf' },
    state: 'PENDING',
    requestedAt: '2026-07-10T00:00:00Z',
    resolvedAt: null,
  };

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should open the accept confirmation dialog', async () => {
    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <AcceptTransferButton namespace="ebf" transfer={transfer}>
        <button type="button">Accept transfer</button>
      </AcceptTransferButton>
    ));

    await user.click(getByRole('button', { name: 'Accept transfer' }));

    const dialog = await waitFor(() => getByRole('alertdialog'));

    expect(dialog).toHaveAccessibleName('Accept ownership transfer');
    expect(dialog).toHaveAccessibleDescription(
      'Are you sure you want to transfer ownership of com.example.group to ebf?',
    );
  });

  test('should submit the accept action successfully', async () => {
    acceptTransfer.mockResolvedValueOnce(undefined);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <AcceptTransferButton namespace="konfigyr" transfer={transfer}>
        <button type="button">Accept transfer</button>
      </AcceptTransferButton>
    ));

    await user.click(getByRole('button', { name: 'Accept transfer' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Accept' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(acceptTransfer).toHaveBeenCalledExactlyOnceWith(transfer.id);
      expect(toast.success).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({
          type: TransferAcceptedSuccessMessage,
          props: expect.objectContaining({ groupId: transfer.groupId }),
        }),
      );
    });
  });

  test('should report a forbidden error clearly', async () => {
    const error = new Error('Only the \'konfigyr\' namespace may accept this transfer');
    acceptTransfer.mockRejectedValueOnce(error);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <AcceptTransferButton namespace="ebf" transfer={transfer}>
        <button type="button">Accept transfer</button>
      </AcceptTransferButton>
    ));

    await user.click(getByRole('button', { name: 'Accept transfer' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Accept' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(acceptTransfer).toHaveBeenCalledExactlyOnceWith(transfer.id);
      expect(errorNotification).toHaveBeenCalledExactlyOnceWith(error);
    });

    expect(toast.success).not.toHaveBeenCalled();
  });
});

describe('components | transfers | <RejectTransferButton/>', () => {
  const transfer: ArtifactOwnershipTransfer = {
    id: 'transfer-1',
    groupId: 'com.example.group',
    from: { id: '1', slug: 'konfigyr' },
    to: { id: '2', slug: 'ebf' },
    state: 'PENDING',
    requestedAt: '2026-07-10T00:00:00Z',
    resolvedAt: null,
  };

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should submit the reject action successfully', async () => {
    rejectTransfer.mockResolvedValueOnce(undefined);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <RejectTransferButton namespace="konfigyr" transfer={transfer}>
        <button type="button">Reject transfer</button>
      </RejectTransferButton>
    ));

    await user.click(getByRole('button', { name: 'Reject transfer' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Reject' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(rejectTransfer).toHaveBeenCalledExactlyOnceWith(transfer.id);
      expect(toast.success).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({
          type: TransferRejectedSuccessMessage,
          props: expect.objectContaining({ groupId: transfer.groupId }),
        }),
      );
    });
  });
});

describe('components | transfers | <CancelTransferButton/>', () => {
  const transfer: ArtifactOwnershipTransfer = {
    id: 'transfer-1',
    groupId: 'com.example.group',
    from: { id: '1', slug: 'konfigyr' },
    to: { id: '2', slug: 'ebf' },
    state: 'PENDING',
    requestedAt: '2026-07-10T00:00:00Z',
    resolvedAt: null,
  };

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should submit the cancel action successfully', async () => {
    cancelTransfer.mockResolvedValueOnce(undefined);

    const user = userEvents.setup();

    const { getByRole } = renderWithQueryClient((
      <CancelTransferButton namespace="ebf" transfer={transfer}>
        <button type="button">Cancel transfer</button>
      </CancelTransferButton>
    ));

    await user.click(getByRole('button', { name: 'Cancel transfer' }));

    const submitButton = await waitFor(() => getByRole('button', { name: 'Cancel request' }));
    await user.click(submitButton);

    await waitFor(() => {
      expect(cancelTransfer).toHaveBeenCalledExactlyOnceWith(transfer.id);
      expect(toast.success).toHaveBeenCalledExactlyOnceWith(
        expect.objectContaining({
          type: TransferCanceledSuccessMessage,
          props: expect.objectContaining({ groupId: transfer.groupId }),
        }),
      );
    });
  });
});
