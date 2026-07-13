import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { GroupVerificationTable } from '@konfigyr/components/groups/group-verification-table';

import type { GroupVerification, PageResponse } from '@konfigyr/hooks/types';

const revokeGroupVerification = vi.hoisted(() => vi.fn());
const errorNotification = vi.hoisted(() => vi.fn());

vi.mock('@konfigyr/hooks', () => ({
  useRevokeGroupVerification: () => ({
    isPending: false,
    mutateAsync: revokeGroupVerification,
  }),
}));

vi.mock('@konfigyr/components/error', () => ({
  useErrorNotification: () => errorNotification,
}));

const createVerification = (overrides: Partial<GroupVerification> = {}): GroupVerification => ({
  id: 'verification-1',
  groupId: 'com.example.group',
  state: 'ACTIVE',
  createdAt: '2026-07-07T00:00:00Z',
  verifiedAt: '2026-07-08T00:00:00Z',
  revokedAt: null,
  ...overrides,
});

const createPageResponse = (
  data: Array<GroupVerification>,
  metadata: PageResponse<GroupVerification>['metadata'] = {
    size: 20,
    number: 1,
    total: data.length,
    pages: 1,
  },
): PageResponse<GroupVerification> => ({
  data,
  metadata,
});

describe('components | groups | <GroupVerificationTable/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render the loading skeleton rows while pending', () => {
    const { container, getByRole } = renderComponentWithRouter((
      <GroupVerificationTable namespace="konfigyr" isPending />
    ));

    expect(getByRole('columnheader', { name: 'Group Id' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Verification state' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Created at' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Verified at' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Actions' })).toBeInTheDocument();

    expect(container.querySelectorAll('[data-slot="group-verification-skeleton"]')).toHaveLength(3);
  });

  test('should render an empty state when no group claims exist', () => {
    const { getByText, queryByRole } = renderComponentWithRouter((
      <GroupVerificationTable
        namespace="konfigyr"
        data={createPageResponse([])}
      />
    ));

    expect(getByText('No group claims found')).toBeInTheDocument();
    expect(getByText('This namespace has not claimed any Maven groupId coordinates yet.')).toBeInTheDocument();
    expect(queryByRole('navigation', { name: 'pagination' })).not.toBeInTheDocument();
  });

  test('should render group verification rows with dates and state badges', () => {
    const data = createPageResponse([
      createVerification(),
      createVerification({
        id: 'verification-2',
        groupId: 'io.github.acme',
        state: 'PENDING',
        createdAt: '2026-07-09T00:00:00Z',
        verifiedAt: null,
      }),
    ]);

    const { getByRole, getByText } = renderComponentWithRouter((
      <GroupVerificationTable
        namespace="konfigyr"
        data={data}
      />
    ));

    const table = getByRole('table');
    expect(table.querySelectorAll('tbody tr')).toHaveLength(2);

    expect(getByText('com.example.group')).toBeInTheDocument();
    expect(getByText('io.github.acme')).toBeInTheDocument();
    expect(getByText('Active')).toBeInTheDocument();
    expect(getByText('Pending')).toBeInTheDocument();
    expect(getByText('Jul 07, 2026')).toBeInTheDocument();
    expect(getByText('Jul 08, 2026')).toBeInTheDocument();
    expect(getByText('Jul 09, 2026')).toBeInTheDocument();
    expect(getByText('—')).toBeInTheDocument();
  });

  test('should show the active row actions menu with revoke actions', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText } = renderComponentWithRouter((
      <GroupVerificationTable
        namespace="konfigyr"
        data={createPageResponse([createVerification()])}
      />
    ));

    await user.click(getByRole('button'));

    await waitFor(() => {
      expect(getByRole('menu')).toBeInTheDocument();
    });

    expect(getByText('View')).toBeInTheDocument();
    expect(getByText('Revoke claim')).toBeInTheDocument();
  });

  test('should show the pending row actions menu with cancel actions', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText } = renderComponentWithRouter((
      <GroupVerificationTable
        namespace="konfigyr"
        data={createPageResponse([
          createVerification({
            id: 'verification-2',
            groupId: 'io.github.acme',
            state: 'PENDING',
            verifiedAt: null,
          }),
        ])}
      />
    ));

    await user.click(getByRole('button'));

    await waitFor(() => {
      expect(getByRole('menu')).toBeInTheDocument();
    });

    expect(getByText('View')).toBeInTheDocument();
    expect(getByText('Cancel claim')).toBeInTheDocument();
  });

  test('shoould render pagination when multiple pages are available', () => {
    const { getByRole } = renderComponentWithRouter((
      <GroupVerificationTable
        namespace="konfigyr"
        page={2}
        size={20}
        data={createPageResponse([
          createVerification({
            id: 'verification-2',
            groupId: 'io.github.acme',
            state: 'PENDING',
            verifiedAt: null,
          }),
        ], {
          size: 20,
          number: 2,
          total: 40,
          pages: 2,
        })}
      />
    ));

    expect(getByRole('navigation', { name: 'pagination' })).toBeInTheDocument();
    expect(getByRole('link', { name: 'Go to previous page' })).toBeInTheDocument();
    expect(getByRole('link', { name: 'Go to next page' })).toBeInTheDocument();
    expect(getByRole('link', { name: '2' })).toHaveAttribute('aria-current', 'page');
  });
});
