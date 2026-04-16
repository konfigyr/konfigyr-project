import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { ChangeRequestStateBadge } from '@konfigyr/components/vault/change-request/change-request-state';

describe('components | vault | change-request | <ChangeRequestStateBadge/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeRequestStateBadge/> component for open state', () => {
    const { container, getByText } = renderWithMessageProvider(
      <ChangeRequestStateBadge value={ChangeRequestState.OPEN}/>,
    );

    expect(getByText('Open')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-pull-request', 'text-emerald-600');
  });

  test('should render <ChangeRequestStateBadge/> component for merged state', () => {
    const { container, getByText } = renderWithMessageProvider(
      <ChangeRequestStateBadge value={ChangeRequestState.MERGED}/>,
    );

    expect(getByText('Merged')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-merge', 'text-blue-600');
  });

  test('should render <ChangeRequestStateBadge/> component for discarded state', () => {
    const { container, getByText } = renderWithMessageProvider(
      <ChangeRequestStateBadge value={ChangeRequestState.DISCARDED}/>,
    );

    expect(getByText('Discarded')).toBeInTheDocument();
    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-pull-request-closed', 'text-destructive');
  });

});
