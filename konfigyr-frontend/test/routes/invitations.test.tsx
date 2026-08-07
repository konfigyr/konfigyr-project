import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor, within } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

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
});
