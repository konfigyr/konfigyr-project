import { afterEach, describe, expect, test } from 'vitest';
import { HttpResponse, http } from 'msw';
import { cleanup, waitFor, within } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import { invitations } from '@konfigyr/test/helpers/mocks';
import { server } from '@konfigyr/test/helpers/server';

describe('routes | invitations', () => {
  afterEach(() => cleanup());

  test('should render the account invitations page', async () => {
    const { getByRole } = renderWithRouter('/invitations');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Invitations' })).toBeInTheDocument();
    });

    expect(getByRole('table')).toBeInTheDocument();
  });

  test('should accept an invitation and remove it from the list without a page reload', async () => {
    const user = userEvents.setup();
    const { getAllByRole, getByText } = renderWithRouter('/invitations');

    await waitFor(() => {
      expect(getAllByRole('row')).toHaveLength(3);
    });

    const rows = getAllByRole('row');
    await user.click(within(rows[1]).getByRole('button', { name: 'Accept' }));

    await waitFor(() => {
      expect(getByText('Successfully joined Konfigyr')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(getAllByRole('row')).toHaveLength(2);
    });
  });

  test('should decline an invitation and remove it from the list without a page reload', async () => {
    const user = userEvents.setup();
    const { getAllByRole, getByText } = renderWithRouter('/invitations');

    await waitFor(() => {
      expect(getAllByRole('row')).toHaveLength(3);
    });

    const rows = getAllByRole('row');
    await user.click(within(rows[2]).getByRole('button', { name: 'Decline' }));

    await waitFor(() => {
      expect(getByText('Declined invitation to John Doe')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(getAllByRole('row')).toHaveLength(2);
    });
  });

  test('should show an actionable empty state once the last invitation is resolved and navigate on demand', async () => {
    server.use(
      http.get('http://localhost/api/account/invitations', () => HttpResponse.json({ data: [invitations.konfigyr] })),
    );

    const user = userEvents.setup();
    const { router, getByRole } = renderWithRouter('/invitations');

    await waitFor(() => {
      expect(getByRole('button', { name: 'Accept' })).toBeInTheDocument();
    });

    await user.click(getByRole('button', { name: 'Accept' }));

    await waitFor(() => {
      expect(getByRole('link', { name: 'Go' })).toHaveAttribute('href', '/namespace/konfigyr');
    });

    await user.click(getByRole('link', { name: 'Go' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr');
    });
  });
});
