import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ChangeRequestHistoryType } from '@konfigyr/hooks/vault/types';
import { ChangeRequestTimeline } from '@konfigyr/components/vault/change-request/change-request-timeline';

import type { ChangeRequestHistory } from '@konfigyr/hooks/vault/types';

const history: ChangeRequestHistory = {
  id: 'history',
  type: ChangeRequestHistoryType.CREATED,
  initiator: 'Jane Doe',
  timestamp: new Date(Date.now() - 1000 * 60 * 6).toISOString(),
};

const render = (type?: ChangeRequestHistoryType) => renderWithMessageProvider(
  <ChangeRequestTimeline history={[{ ...history, type: type ?? history.type }]} />,
);

describe('components | vault | change-request | <ChangeRequestTimeline/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeRequestTimeline/> component for created event', () => {
    const { container } = render();

    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-pull-request-create');

    expect(container).toHaveTextContent('Jane Doe opened the change request 6 minutes ago.');
  });

  test('should render <ChangeRequestTimeline/> component for renamed event', () => {
    const { container } = render(ChangeRequestHistoryType.RENAMED);

    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-folder-pen');

    expect(container).toHaveTextContent('Jane Doe renamed this change request 6 minutes ago.');
  });

  test('should render <ChangeRequestTimeline/> component for commented event', () => {
    const { container } = render(ChangeRequestHistoryType.COMMENTED);

    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-message-circle');

    expect(container).toHaveTextContent('Jane Doe commented on this change request 6 minutes ago.');
  });

  test('should render <ChangeRequestTimeline/> component for changes request event', () => {
    const { container } = render(ChangeRequestHistoryType.CHANGES_REQUESTED);

    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-message-circle-plus');

    expect(container).toHaveTextContent('Jane Doe requested additional changes 6 minutes ago.');
  });

  test('should render <ChangeRequestTimeline/> component for approved event', () => {
    const { container } = render(ChangeRequestHistoryType.APPROVED);

    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-circle-check-big');

    expect(container).toHaveTextContent('Jane Doe approved this change request 6 minutes ago.');
  });

  test('should render <ChangeRequestTimeline/> component for discarded event', () => {
    const { container } = render(ChangeRequestHistoryType.DISCARDED);

    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-pull-request-closed');

    expect(container).toHaveTextContent('Jane Doe discared changes 6 minutes ago.');
  });

  test('should render <ChangeRequestTimeline/> component for merged event', () => {
    const { container } = render(ChangeRequestHistoryType.MERGED);

    expect(container.querySelector('svg')).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveClass('lucide-git-merge');

    expect(container).toHaveTextContent('Jane Doe merged this change request 6 minutes ago.');
  });

});
