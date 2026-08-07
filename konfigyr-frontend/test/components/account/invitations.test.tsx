import { afterEach, describe, expect, test } from 'vitest';
import { HttpResponse, http } from 'msw';
import { cleanup, waitFor, within } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { AccountInvitations } from '@konfigyr/components/account/invitations';
import { Toaster } from '@konfigyr/components/ui/toast';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { invitations } from '@konfigyr/test/helpers/mocks';
import { server } from '@konfigyr/test/helpers/server';

import type { Invitation } from '@konfigyr/hooks/types';

const render = (data: Array<Invitation>) => renderWithQueryClient((
  <>
    <AccountInvitations invitations={data} />
    <Toaster />
  </>
));

describe('components | account | <AccountInvitations/>', () => {
  afterEach(() => cleanup());

  test('should render empty state when there are no pending invitations', () => {
    const { getByRole, getByText } = render([]);

    expect(getByRole('table')).toBeInTheDocument();
    expect(getByText('No pending invitations')).toBeInTheDocument();
  });

  test('should render invitations spanning multiple namespaces', () => {
    const { getAllByRole, getByRole } = render([invitations.konfigyr, invitations.johnDoe]);

    const table = getByRole('table');
    const headers = within(table).getAllByRole('columnheader');
    expect(headers).toHaveLength(6);
    expect(headers[0]).toHaveTextContent('Namespace');
    expect(headers[1]).toHaveTextContent('Role');
    expect(headers[2]).toHaveTextContent('Sender');
    expect(headers[3]).toHaveTextContent('Created at');
    expect(headers[4]).toHaveTextContent('Expires at');

    const rows = getAllByRole('row');
    expect(rows).toHaveLength(3);

    const first = within(rows[1]).getAllByRole('cell');
    expect(first[0]).toHaveTextContent('Konfigyr');
    expect(first[1]).toHaveTextContent('User');
    expect(first[2]).toHaveTextContent('John Doejohn.doe@konfigyr.com');
    expect(first[3]).toHaveTextContent('4/17/2026');
    expect(first[4]).toHaveTextContent('4/28/2026');

    const second = within(rows[2]).getAllByRole('cell');
    expect(second[0]).toHaveTextContent('John Doe');
    expect(second[1]).toHaveTextContent('Administrator');
    expect(second[2]).toHaveTextContent('Jane Doejane.doe@konfigyr.com');
    expect(second[3]).toHaveTextContent('4/11/2026');
    expect(second[4]).toHaveTextContent('4/17/2026');
  });

  test('should accept an invitation and show a success toast', async () => {
    const user = userEvents.setup();
    const result = render([invitations.konfigyr, invitations.johnDoe]);

    const rows = result.getAllByRole('row');
    await user.click(within(rows[1]).getByRole('button', { name: 'Accept' }));

    await waitFor(() => {
      expect(result.getByText('Successfully joined Konfigyr')).toBeInTheDocument();
    });
  });

  test('should show an error toast when accepting an invitation fails', async () => {
    server.use(
      http.post('http://localhost/api/account/invitations/:key', () => (
        HttpResponse.json({
          status: 409,
          title: 'Conflict',
          detail: 'This invitation has already been resolved.',
        }, { status: 409 })
      )),
    );

    const user = userEvents.setup();
    const result = render([invitations.konfigyr]);

    await user.click(result.getByRole('button', { name: 'Accept' }));

    await waitFor(() => {
      expect(result.getByText('Conflict')).toBeInTheDocument();
    });
  });

  test('should decline an invitation and show a success toast', async () => {
    const user = userEvents.setup();
    const result = render([invitations.konfigyr, invitations.johnDoe]);

    const rows = result.getAllByRole('row');
    await user.click(within(rows[2]).getByRole('button', { name: 'Decline' }));

    await waitFor(() => {
      expect(result.getByText('Declined invitation to John Doe')).toBeInTheDocument();
    });
  });

  test('should show an error toast when declining an invitation fails', async () => {
    server.use(
      http.delete('http://localhost/api/account/invitations/:key', () => (
        HttpResponse.json({
          status: 410,
          title: 'Gone',
          detail: 'This invitation has expired.',
        }, { status: 410 })
      )),
    );

    const user = userEvents.setup();
    const result = render([invitations.konfigyr]);

    await user.click(result.getByRole('button', { name: 'Decline' }));

    await waitFor(() => {
      expect(result.getByText('Gone')).toBeInTheDocument();
    });
  });
});
