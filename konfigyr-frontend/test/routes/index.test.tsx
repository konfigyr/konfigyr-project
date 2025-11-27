import { expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from 'konfigyr-test/helpers/router';

test('renders the home page', async () => {
  const { getByRole } = renderWithRouter('/');

  await waitFor(() => {
    expect(getByRole('heading', { name: 'Welcome to Konfigyr', level: 3 })).toBeInTheDocument();
  });
});
