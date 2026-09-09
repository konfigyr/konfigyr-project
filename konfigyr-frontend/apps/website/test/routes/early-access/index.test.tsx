import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | early-access | index', () => {
  test('renders the early access page with its form', async () => {
    const { getByRole, router } = renderWithRouter('/early-access');

    await waitFor(() => {
      expect(router.state.status).toStrictEqual('idle');
    });

    expect(getByRole('heading', { level: 1, name: 'Request Early Access' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Request Early Access' })).toBeInTheDocument();
  });
});
