import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | imprint', () => {
  test('renders the imprint page', async () => {
    const { getByRole, router } = renderWithRouter('/imprint');

    await waitFor(() => {
      expect(router.state.status).toStrictEqual('idle');
    });

    expect(getByRole('heading', { level: 1, name: 'Imprint' })).toBeInTheDocument();
  });
});
