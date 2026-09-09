import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | not-found', () => {
  test('renders the default not found page', async () => {
    const { getByRole, router } = renderWithRouter('/not-found');

    await waitFor(() => {
      expect(router.state.status).toStrictEqual('idle');
    });

    expect(getByRole('heading', { name: 'Page not found', level: 1 })).toBeInTheDocument();
    expect(getByRole('link', { name: 'Back to homepage' })).toHaveAttribute('href', '/');
  });
});
