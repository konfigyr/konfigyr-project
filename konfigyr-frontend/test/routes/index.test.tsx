import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | index', () => {
  test('renders the home page', async () => {
    const { getByRole, getByText } = renderWithRouter('/');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Welcome to Konfigyr', level: 3 })).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(getByText('Hello: John Doe')).toBeInTheDocument();
    });
  });
});
