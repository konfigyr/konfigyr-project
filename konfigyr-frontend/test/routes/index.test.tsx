import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | index', () => {
  afterEach(() => cleanup());

  test('should redirect user to Namespace provisioning page when Account has no memberships', async () => {
    const { router } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/provision');
    });
  });
});
