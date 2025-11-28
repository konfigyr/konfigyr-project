import { afterEach, describe, expect, test, vi } from 'vitest';
import { proxy } from '@konfigyr/lib/api-proxy';
import { updateSessionAccessToken } from '@konfigyr/test/helpers/session';

describe('services | api-proxy', () => {
  afterEach(() => {
    vi.resetAllMocks();
  });

  test('should respond with 401 status code when no access token is present in the session', async () => {
    const response = await proxy(new Request('http://localhost/api/test-uri'));

    expect(response.status).toStrictEqual(401);
    expect(await response.json()).toMatchObject({
      status: 401,
      title: 'Not authenticated',
      detail: 'You need to login to access this resource.',
    });
  });

  test('should respond with 500 status code due to runtime errors', async () => {
    await updateSessionAccessToken();

    const response = await proxy(new Request('http://localhost/api/proxy-test/network-error'));

    expect(response.status).toStrictEqual(500);
    expect(await response.json()).toMatchObject({
      status: 500,
      title: 'Internal Server Error',
      detail: 'An unexpected error occurred while proxying the request.',
    });
  });

  test('should proxy target service error response', async () => {
    await updateSessionAccessToken();

    const response = await proxy(new Request('http://localhost/api/proxy-test/server-error'));

    expect(response.status).toStrictEqual(500);
    expect(await response.json()).toMatchObject({
      status: 500,
      title: 'Internal server error',
      detail: 'Unexpected error occurred.',
    });
  });

  test('should proxy target service successful response', async () => {
    await updateSessionAccessToken();

    const response = await proxy(new Request('http://localhost/api/proxy-test/create-resource', {
      method: 'POST',
      body: JSON.stringify({ name: 'test-resource' }),
      headers: { 'content-type': 'application/json' },
    }));

    expect(response.status).toStrictEqual(200);
    expect(await response.json()).toMatchObject({
      id: 'test-resource-id',
      name: 'test-resource',
    });
  });

  test('should sanitize request headers before they are sent to target service', async () => {
    await updateSessionAccessToken();

    const response = await proxy(new Request('http://localhost/api/proxy-test/verify-headers', {
      method: 'DELETE',
      headers: {
        'x-request-id': 'request-id',
        'content-type': 'application/json',
        'host': 'original.host',
        'cookie': 'original.cookie',
      },
    }));

    expect(response.status).toStrictEqual(204);
    expect(await response.text()).toStrictEqual('');
  });

  [502, 503, 504].forEach((status) => {
    test(`should respond with ${status} unavailable status code when target service is unavailable`, async () => {
      await updateSessionAccessToken();

      const body = new FormData();
      body.append('status', `${status}`);

      const response = await proxy(new Request('http://localhost/api/proxy-test/unavailable', {
        method: 'PATCH', body,
      }));

      expect(response.status).toStrictEqual(status);
      expect(await response.json()).toMatchObject({
        status,
        title: 'Unavailable',
        detail: 'The server is currently unavailable, please try again later.',
      });
    });
  });

});
