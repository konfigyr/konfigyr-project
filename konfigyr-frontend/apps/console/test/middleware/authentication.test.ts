// @vitest-environment node

import { afterEach, describe, expect, test, vi } from 'vitest';
import { authenticationMiddleware } from '@konfigyr/middleware/authentication';
import { updateSessionAccessToken } from '@konfigyr/test/helpers/session';
import { getRequestUrl } from '@tanstack/react-start/server';

const setupRequestUrl = (path: string = '') => {
  const url = new URL(`http://localhost${path}`);

  // @ts-expect-error: this is mocked by the @konfigyr/test/helpers/session helper
  getRequestUrl.mockReturnValue(url);
};

describe('middleware | authentication', () => {
  const middleware = authenticationMiddleware();
  const next = vi.fn();

  afterEach(() => {
    vi.resetAllMocks();
  });

  test('should ignore non-protected routes', async () => {
    setupRequestUrl('/api/namespaces');

    // @ts-expect-error: the type does not define the `server` method
    await middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any);

    expect(next).toHaveBeenCalled();
  });

  test('should ignore protected routes when authenticated', async () => {
    setupRequestUrl('/namespaces');

    await updateSessionAccessToken();

    // @ts-expect-error: the type does not define the `server` method
    await middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any);

    expect(next).toHaveBeenCalled();
  });

  test('should redirect to Konfigyr OIDC server when not authenticated', async () => {
    setupRequestUrl('/namespaces');

    // @ts-expect-error: the type does not define the `server` method
    await expect(() => middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any)).rejects.toThrow(Response);

    expect(next).not.toHaveBeenCalled();
  });

  test('should redirect to Konfigyr OIDC server when refresh-token is not specified', async () => {
    setupRequestUrl('/namespaces');

    await updateSessionAccessToken({
      accessToken: 'expired-access-token',
      refreshToken: undefined,
      expiresAt: Date.now() - 600000,
    });

    // @ts-expect-error: the type does not define the `server` method
    await expect(() => middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any)).rejects.toThrow(Response);

    expect(next).not.toHaveBeenCalled();
  });

  test('should attempt to refresh the access token', async () => {
    setupRequestUrl('/namespaces');

    await updateSessionAccessToken({
      accessToken: 'expired-access-token',
      expiresAt: Date.now() - 600000,
    });

    // @ts-expect-error: the type does not define the `server` method
    await middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any);

    expect(next).toHaveBeenCalled();
  });

});
