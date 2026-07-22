import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { audit, namespaces, transfers } from '@konfigyr/test/helpers/mocks';
import { TransferDetails } from '@konfigyr/components/artifactory/transfers/transfer-details';

import type { ArtifactOwnershipTransfer } from '@konfigyr/hooks/types';

describe('components | transfers | <TransferDetails/>', () => {
  afterEach(() => cleanup());

  test('should show Accept/Reject and not Cancel when this namespace is the from party', () => {
    const { getByRole, queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespaces.konfigyr} transfer={transfers.incomingPending}/>,
    );

    expect(getByRole('button', { name: 'Accept' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Reject' })).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel request' })).not.toBeInTheDocument();
  });

  test('should show Cancel and not Accept/Reject when this namespace is the to party', () => {
    const transfer: ArtifactOwnershipTransfer = {
      ...transfers.incomingPending,
      from: { id: '99', slug: 'ebf' },
      to: { id: '2', slug: namespaces.konfigyr.slug },
    };

    const { getByRole, queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespaces.konfigyr} transfer={transfer}/>,
    );

    expect(getByRole('button', { name: 'Cancel request' })).toBeInTheDocument();
    expect(queryByRole('button', { name: 'Accept' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
  });

  test('should not show any actions for a resolved transfer', () => {
    const transfer: ArtifactOwnershipTransfer = {
      ...transfers.incomingPending,
      state: 'ACCEPTED',
      resolvedAt: '2026-07-11T00:00:00Z',
    };

    const { queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespaces.konfigyr} transfer={transfer}/>,
    );

    expect(queryByRole('button', { name: 'Accept' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel request' })).not.toBeInTheDocument();
  });

  test('should not show any actions when this namespace is neither party', () => {
    const transfer: ArtifactOwnershipTransfer = {
      ...transfers.incomingPending,
      from: { id: '99', slug: 'ebf' },
      to: { id: '100', slug: 'other-namespace' },
    };

    const { queryByRole } = renderWithQueryClient(
      <TransferDetails namespace={namespaces.konfigyr} transfer={transfer}/>,
    );

    expect(queryByRole('button', { name: 'Accept' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
    expect(queryByRole('button', { name: 'Cancel request' })).not.toBeInTheDocument();
  });

  test('should show the audit trail scoped to this transfer', async () => {
    const { getByRole, getByText } = renderWithQueryClient(
      <TransferDetails namespace={namespaces.konfigyr} transfer={transfers.incomingPending}/>,
    );

    await waitFor(() => {
      expect(getByRole('heading', { name: 'History' })).toBeInTheDocument();
      expect(getByText(audit.transferRequested.message)).toBeInTheDocument();
      expect(getByText(/John Doe/)).toBeInTheDocument();
    });
  });
});
