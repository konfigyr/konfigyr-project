import { afterEach,beforeEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { TransferDetails } from '@konfigyr/components/artifactory/transfers/transfer-details';

import type { ArtifactOwnershipTransfer, AuditRecord, CursorResponse, Namespace } from '@konfigyr/hooks/types';

const acceptTransfer = vi.hoisted(() => vi.fn());
const rejectTransfer = vi.hoisted(() => vi.fn());
const cancelTransfer = vi.hoisted(() => vi.fn());
const errorNotification = vi.hoisted(() => vi.fn());
const useGetAuditRecords = vi.hoisted(() => vi.fn());

vi.mock('@konfigyr/hooks', () => ({
  useAcceptTransfer: () => ({ isPending: false, mutateAsync: acceptTransfer }),
  useRejectTransfer: () => ({ isPending: false, mutateAsync: rejectTransfer }),
  useCancelTransfer: () => ({ isPending: false, mutateAsync: cancelTransfer }),
  useGetAuditRecords,
}));

vi.mock('@konfigyr/components/error', () => ({
  useErrorNotification: () => errorNotification,
  ErrorState: () => null,
}));

const namespace: Namespace = {
  id: '2',
  slug: 'konfigyr',
  name: 'Konfigyr',
  description: 'Konfigyr namespace',
};

const baseTransfer: ArtifactOwnershipTransfer = {
  id: 'transfer-1',
  groupId: 'com.example.group',
  from: { id: '1', slug: namespace.slug },
  to: { id: '2', slug: 'ebf' },
  state: 'PENDING',
  requestedAt: '2026-07-10T00:00:00Z',
  resolvedAt: null,
};

const emptyAuditRecords: CursorResponse<AuditRecord> = { data: [], metadata: { size: 20 } };

describe('components | transfers | <TransferDetails/>', () => {
  beforeEach(() => {
    useGetAuditRecords.mockReturnValue({ data: emptyAuditRecords, error: null, isPending: false });
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should show Accept/Reject and not Cancel when this namespace is the from party', () => {
    const transfer: ArtifactOwnershipTransfer = { ...baseTransfer, from: { id: '1', slug: namespace.slug }, to: { id: '2', slug: 'ebf' } };

    const { getByRole, queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespace} transfer={transfer}/>,
    );

    expect(getByRole('button', { name: 'Accept' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Reject' })).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel request' })).not.toBeInTheDocument();
  });

  test('should show Cancel and not Accept/Reject when this namespace is the to party', () => {
    const transfer: ArtifactOwnershipTransfer = { ...baseTransfer, from: { id: '1', slug: 'ebf' }, to: { id: '2', slug: namespace.slug } };

    const { getByRole, queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespace} transfer={transfer}/>,
    );

    expect(getByRole('button', { name: 'Cancel request' })).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Accept' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
  });

  test('should not show any actions for a resolved transfer', () => {
    const transfer: ArtifactOwnershipTransfer = {
      ...baseTransfer,
      state: 'ACCEPTED',
      resolvedAt: '2026-07-11T00:00:00Z',
    };

    const { queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespace} transfer={transfer}/>,
    );

    expect(queryByRole('button', { name: 'Accept' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel request' })).not.toBeInTheDocument();
  });

  test('should not show any actions when this namespace is neither party', () => {
    const transfer: ArtifactOwnershipTransfer = {
      ...baseTransfer,
      from: { id: '1', slug: 'ebf' },
      to: { id: '2', slug: 'other-namespace' },
    };

    const { queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespace} transfer={transfer}/>,
    );

    expect(queryByRole('button', { name: 'Accept' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel request' })).not.toBeInTheDocument();
  });

  test('should show the audit trail scoped to this transfer', async () => {
    const records: CursorResponse<AuditRecord> = {
      data: [{
        id: 'transfer-requested-audit-record',
        entityType: 'artifact-ownership-transfer',
        entityId: baseTransfer.id,
        eventType: 'artifact-ownership-transfer.requested',
        message: 'Ownership transfer for com.example.group was requested from ebf',
        actor: { id: 'john-doe', type: 'USER_ACCOUNT', name: 'John Doe' },
        createdAt: '2026-07-10T00:00:00Z',
      }],
      metadata: { size: 20 },
    };

    useGetAuditRecords.mockReturnValue({ data: records, error: null, isPending: false });

    const { getByRole, getByText } = renderWithQueryClient(
      <TransferDetails namespace={namespace} transfer={baseTransfer}/>,
    );

    await waitFor(() => {
      expect(getByRole('heading', { name: 'History' })).toBeInTheDocument();
      expect(getByText('Ownership transfer for com.example.group was requested from ebf')).toBeInTheDocument();
      expect(getByText(/John Doe/)).toBeInTheDocument();
    });
  });
});
