import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event';

describe('routes | namespace | audit', () => {
  afterEach(() => cleanup());

  test('should render Namespace audit record list page', async () => {
    const user = userEvents.setup();
    const { getByRole, getByText, router } = renderWithRouter('/namespace/konfigyr/audit');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Audit logs' })).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(getByText('Namespace was created')).toBeInTheDocument();
    });

    expect(getByRole('listitem', { name: 'Keyset was created' })).toBeInTheDocument();
    expect(getByRole('listitem', { name: 'Service was created' })).toBeInTheDocument();
    expect(getByRole('listitem', { name: 'Profile was created' })).toBeInTheDocument();
    expect(getByRole('listitem', { name: 'Namespace was created' })).toBeInTheDocument();

    expect(getByRole('button', { name: 'Go to next page' })).toBeInTheDocument();

    await user.click(
      getByRole('button', { name: 'Go to next page' }),
    );

    await waitFor(() => {
      expect(router.state.location.search).toMatchObject({
        token: 'next-token', size: 20,
      });
    });
  });
});
