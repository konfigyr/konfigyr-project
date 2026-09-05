import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { TransferStateBadge, TransferStateLabel } from '@konfigyr/components/artifactory/transfers/transfer-state-badge';

describe('components | transfers | <TransferStateLabel/>', () => {
  afterEach(() => cleanup());

  test('should render a label for a pending transfer', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateLabel state="PENDING" />,
    );

    expect(getByText('Pending')).toBeInTheDocument();
  });

  test('should render a label for an accepted transfer', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateLabel state="ACCEPTED" />,
    );

    expect(getByText('Accepted')).toBeInTheDocument();
  });

  test('should render a label for a rejected transfer', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateLabel state="REJECTED" />,
    );

    expect(getByText('Rejected')).toBeInTheDocument();
  });

  test('should render a label for a cancelled transfer', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateLabel state="CANCELLED" />,
    );

    expect(getByText('Cancelled')).toBeInTheDocument();
  });

  test('should render nothing for an unknown state', () => {
    const { container } = renderWithMessageProvider(
      <TransferStateLabel state="UNKNOWN" />,
    );

    expect(container).toBeEmptyDOMElement();
  });
});

describe('components | transfers | <TransferStateBadge/>', () => {
  afterEach(() => cleanup());

  test('should render a pending transfer badge', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateBadge state="PENDING" data-testid="badge" />,
    );

    expect(getByText('Pending')).toBeInTheDocument();
    expect(getByText('Pending').closest('[data-testid="badge"]')).toHaveClass('bg-warning/10');
  });

  test('should render an accepted transfer badge', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateBadge state="ACCEPTED" data-testid="badge" />,
    );

    expect(getByText('Accepted')).toBeInTheDocument();
    expect(getByText('Accepted').closest('[data-testid="badge"]')).toHaveClass('bg-success/10');
  });

  test('should render a rejected transfer badge', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateBadge state="REJECTED" data-testid="badge" />,
    );

    expect(getByText('Rejected')).toBeInTheDocument();
    expect(getByText('Rejected').closest('[data-testid="badge"]')).toHaveClass('bg-destructive/10');
  });

  test('should render a cancelled transfer badge', () => {
    const { getByText } = renderWithMessageProvider(
      <TransferStateBadge state="CANCELLED" data-testid="badge" />,
    );

    expect(getByText('Cancelled')).toBeInTheDocument();
    expect(getByText('Cancelled').closest('[data-testid="badge"]')).toHaveClass('bg-secondary');
  });

  test('should render nothing for an unknown state', () => {
    const { queryByTestId } = renderWithMessageProvider(
      <TransferStateBadge state="UNKNOWN" data-testid="badge" />,
    );

    expect(queryByTestId('badge')).not.toBeInTheDocument();
  });
});
