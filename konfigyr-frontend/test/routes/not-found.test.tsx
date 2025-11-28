import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | not-found', () => {
  test('renders the default not found page', async () => {
    const { getByRole } = renderWithRouter('/not-found');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'The page you are looking for does not exist.', level: 3 }))
        .toBeInTheDocument();
    });
  });
});
