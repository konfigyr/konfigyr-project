import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ChangeHistoryTimeline } from '@konfigyr/components/vault/change-history/change-history-timeline';

import type { ChangeHistory } from '@konfigyr/hooks/types';

describe('components | vault | change-history | <ChangeHistoryTimeline/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeHistoryTimeline/> component in loading state', () => {
    const { container, queryByRole } = renderWithMessageProvider(
      <ChangeHistoryTimeline isPending={true} />,
    );

    expect(container.querySelector('[data-slot="changeset-history-skeleton"]'))
      .toBeInTheDocument();

    expect(queryByRole('alert')).not.toBeInTheDocument();
    expect(queryByRole('list')).not.toBeInTheDocument();
  });

  test('should render <ChangeHistoryTimeline/> component in error state', () => {
    const { queryByRole, getByRole } = renderWithMessageProvider(
      <ChangeHistoryTimeline error={new Error('Ooops, failed to load history')} />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(queryByRole('list')).not.toBeInTheDocument();
  });

  test('should render <ChangeHistoryTimeline/> component with history records', () => {
    const history: Array<ChangeHistory> = [{
      id: 'second-changeset',
      revision: 'second-changeset',
      subject: 'Second change',
      count: 3,
      appliedBy: 'Jane Doe',
      appliedAt: new Date(Date.now() - 4 * 24 * 60 * 60 * 1000).toISOString(),
    }, {
      id: 'first-changeset',
      revision: 'first-changeset',
      subject: 'First change',
      count: 12,
      appliedBy: 'John Doe',
      appliedAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
    }];

    const { getByText, getByRole } = renderWithMessageProvider(
      <ChangeHistoryTimeline history={history} />,
    );

    expect(getByRole('listitem', { name: 'First change' })).toBeInTheDocument();
    expect(getByRole('listitem', { name: 'Second change' })).toBeInTheDocument();

    expect(getByText('Jane Doe')).toBeInTheDocument();
    expect(getByText('3 changes')).toBeInTheDocument();
    expect(getByText('Second change')).toBeInTheDocument();
    expect(getByText('second-changeset')).toBeInTheDocument();
    expect(getByText('4 days ago')).toBeInTheDocument();

    expect(getByText('John Doe')).toBeInTheDocument();
    expect(getByText('12 changes')).toBeInTheDocument();
    expect(getByText('First change')).toBeInTheDocument();
    expect(getByText('first-changeset')).toBeInTheDocument();
    expect(getByText('5 days ago')).toBeInTheDocument();
  });
});
