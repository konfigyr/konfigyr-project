import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | not-found', () => {
  test('renders the default not found page', async () => {
    const { getByRole, router } = renderWithRouter('/not-found');

    await waitFor(() => {
      expect(router.state.statusCode).toStrictEqual(404);
    });

    expect(getByRole('heading', { name: 'Resource Not Found', level: 3 }))
      .toBeInTheDocument();

    expect(getByRole('button', { name: 'Go back' })).toBeInTheDocument();

    expect(getByRole('link', { name: 'Home' })).toBeInTheDocument();
    expect(getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
  });
});
