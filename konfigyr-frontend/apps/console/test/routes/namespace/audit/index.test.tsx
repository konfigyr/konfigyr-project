import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event';

describe('routes | namespace | audit', () => {
  afterEach(() => cleanup());

  test('should sync pagination to the URL search params', async () => {
    const user = userEvents.setup();
    const { getByRole, router } = renderWithRouter('/namespace/konfigyr/audit');

    await waitFor(() => {
      expect(getByRole('button', { name: 'Go to next page' })).toBeInTheDocument();
    });

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
