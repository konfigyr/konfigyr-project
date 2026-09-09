import { describe, expect, test } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | index', () => {
  test('renders the homepage', async () => {
    const { getByRole, router } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.status).toStrictEqual('idle');
    });

    expect(getByRole('heading', {
      level: 1,
      name: 'Configuration management for Spring Boot.',
    })).toBeInTheDocument();
  });
});
