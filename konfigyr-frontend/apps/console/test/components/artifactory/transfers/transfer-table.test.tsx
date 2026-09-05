import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { transfers } from '@konfigyr/test/helpers/mocks';
import { TransferTable } from '@konfigyr/components/artifactory/transfers/transfer-table';

import type { ArtifactOwnershipTransfer, PageResponse } from '@konfigyr/hooks/types';

function page(data: Array<ArtifactOwnershipTransfer>): PageResponse<ArtifactOwnershipTransfer> {
  return { data, metadata: { number: 1, size: 20, total: data.length, pages: 1 } };
}

describe('components | transfers | <TransferTable/>', () => {
  afterEach(() => cleanup());

  test('should render incoming transfers with the counterpart namespace', () => {
    const { getByText } = renderComponentWithRouter(
      <TransferTable namespace="konfigyr" direction="incoming" data={page([transfers.incomingPending])} isPending={false}/>,
    );

    expect(getByText('com.example.group')).toBeInTheDocument();
    expect(getByText('ebf')).toBeInTheDocument();
    expect(getByText('Pending')).toBeInTheDocument();
  });

  test('should render outgoing transfers with the counterpart namespace', () => {
    const { getByText, queryByText } = renderComponentWithRouter(
      <TransferTable namespace="konfigyr" direction="outgoing" data={page([transfers.outgoingPending])} isPending={false}/>,
    );

    expect(getByText('io.github.acme')).toBeInTheDocument();
    expect(getByText('ebf')).toBeInTheDocument();
    expect(queryByText('com.example.group')).not.toBeInTheDocument();
  });

  test('should render an empty state when no transfers exist', () => {
    const { getByText } = renderComponentWithRouter(
      <TransferTable namespace="konfigyr" direction="incoming" data={page([])} isPending={false}/>,
    );

    expect(getByText('No ownership transfers found')).toBeInTheDocument();
  });
});
