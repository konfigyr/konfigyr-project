import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | index', () => {
  afterEach(() => cleanup());

  test('should redirect user to first available Namespace from Account memebership', async () => {
    const { router } = renderWithRouter('/');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr');
    });
  });
});
