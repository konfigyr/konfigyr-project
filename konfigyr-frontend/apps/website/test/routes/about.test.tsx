import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | about', () => {
  test('renders the about page', async () => {
    const { getByRole, router } = renderWithRouter('/about');

    await waitFor(() => {
      expect(router.state.status).toStrictEqual('idle');
    });

    expect(getByRole('heading', { level: 1, name: 'Why we\'re building Konfigyr' })).toBeInTheDocument();
  });
});
