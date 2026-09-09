import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | privacy', () => {
  test('renders the privacy policy page', async () => {
    const { getByRole, router } = renderWithRouter('/privacy');

    await waitFor(() => {
      expect(router.state.status).toStrictEqual('idle');
    });

    expect(getByRole('heading', { level: 1, name: 'Privacy Policy' })).toBeInTheDocument();
  });
});
