import { HTTPError } from 'ky';
import { afterEach, describe, expect, test, vi } from 'vitest';
import request, { resolvePrefixUrl } from '@konfigyr/lib/http';

describe('services | http', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  test('should resolve prefix URL based on environment', () => {
    vi.stubEnv('KONFIGYR_HTTP_HOST', 'https://stubbed.com');

    expect(resolvePrefixUrl()).toBe('https://stubbed.com');
  });

  test('should resolve prefix URL based on the current window location', () => {
    vi.stubEnv('KONFIGYR_HTTP_HOST', undefined);
    vi.stubGlobal('window', { location: { origin: 'https://origin.com' } });

    expect(resolvePrefixUrl()).toBe('https://origin.com');
  });

  test('should resolve prefix URL based when no environment variable or window location is present', () => {
    vi.stubEnv('KONFIGYR_HTTP_HOST', undefined);
    vi.stubGlobal('window', undefined);

    expect(resolvePrefixUrl()).toBe('/');
  });

  test('should retrieve account information', async () => {
    const account = await request.get('api/account').json<Record<string, unknown>>();

    expect(account).toMatchObject({
      id: '06Y7W2BYKG9B9',
      email: 'john.doe@konfigyr.com',
      firstName: 'John',
      lastName: 'Doe',
      fullName: 'John Doe',
    });
  });

  test('should adapt HTTP error into a problem detail', async () => {
    expect.assertions(2);

    try {
      await request.get(new URL('https://api.konfigyr.com/proxy-test/problem-details')).json();
    } catch (error) {
      expect(error).toBeInstanceOf(HTTPError);
      expect((error as HTTPError).problem).toStrictEqual({
        status: 500,
        type: 'https://example.com/problem/out-of-credit',
        title: 'Problem detail title',
        detail: 'Problem detail description.',
        instance: 'https://api.konfigyr.com/proxy-test/problem-details',
        errors: [{
          detail: 'Insufficient funds.',
          pointer: 'balance',
        }],
      });
    }
  });

  test('should fail to adapt HTTP error into a problem detail', async () => {
    expect.assertions(2);

    try {
      await request.get(new URL('https://api.konfigyr.com/proxy-test/text-error-response')).json();
    } catch (error) {
      expect(error).toBeInstanceOf(HTTPError);
      expect((error as HTTPError).problem).toBeUndefined();
    }
  });

  test('should fail to execute request due to network error', async () => {
    await expect(request.get(new URL('https://api.konfigyr.com/proxy-test/network-error')).json())
      .rejects.toThrow('Failed to fetch');
  });
});
