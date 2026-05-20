import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | join namespace', () => {
  afterEach(() => cleanup());

  test('should render the join namespace page and accept invitation', async () => {
    const { router, getByRole, getByText } = renderWithRouter('/join/konfigyr/0b9f514567f6cd9bb393a06388fc3dd7');

    await waitFor(() => {
      expect(getByText('You\'ve been invited')).toBeInTheDocument();
    });

    expect(getByText('John Doe has a seat waiting for you.')).toBeInTheDocument();
    expect(getByRole('button', { name: 'Join Konfigyr' })).toBeInTheDocument();

    await userEvent.click(
      getByRole('button', { name: 'Join Konfigyr' }),
    );

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr');
    });
  });
});
