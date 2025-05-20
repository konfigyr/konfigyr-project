import {afterEach, describe, expect, Mock, test, vi } from 'vitest';
import { NextRequest } from 'next/server';
import * as route from 'konfigyr/app/api/[[...slug]]/route';

vi.mock('konfigyr/services/identity', () => {
  return {
    get: () => Promise.resolve(),
  };
});

const createMockedResponse = (status: number, body: unknown, headers = {}) => {
  global.fetch = vi.fn(() => Promise.resolve({
    status,
    headers: headers,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(body),
  })) as Mock;
};

const createMockedAccessToken = async (token: string) => {
  const tokens = await import('konfigyr/services/identity');

  tokens.get = vi.fn().mockReturnValue(Promise.resolve({
    email: 'john.doe@konfigyr.com', token,
  }));
};

describe('app/api', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  test('should respond with 401 status code when no access token is present in the session', async () => {
    global.fetch = vi.fn();

    const request = new NextRequest('http://localhost/api/test-uri');
    const response = await route.GET(request);

    expect(response.status).toStrictEqual(401);
    expect(await response.json()).toMatchObject({
      status: 401,
      title: 'Not authenticated',
      detail: 'You need to login to access this resource',
      instance: '/api/test-uri',
    });

    expect(fetch).not.toHaveBeenCalled();
  });

  test('should respond with 500 status code due to runtime errors', async () => {
    await createMockedAccessToken('access-token');
    global.fetch = vi.fn().mockReturnValue(Promise.reject(new Error('Oooops')));

    const request = new NextRequest('http://localhost/api/test-uri');
    const response = await route.GET(request);

    expect(response.status).toStrictEqual(500);
    expect(await response.json()).toMatchObject({
      status: 500,
      title: 'Internal server error',
      detail: 'Unexpected error occurred while processing your request',
      instance: '/api/test-uri',
    });

    expect(fetch).toHaveBeenCalled();
  });

  test('should proxy target service error response', async () => {
    await createMockedAccessToken('access-token');
    createMockedResponse(400, '{"status": 400, "title": "Bad request"}');

    const request = new NextRequest('http://localhost/api/nested/test-uri', { method: 'DELETE' });
    const response = await route.DELETE(request);

    expect(response.status).toStrictEqual(400);
    expect(await response.json()).toMatchObject({
      status: 400,
      title: 'Bad request',
    });

    const headers = new Headers(request.headers);
    headers.set('Authorization', 'Bearer access-token');

    expect(fetch).toHaveBeenCalledWith('https://api.konfigyr.com/nested/test-uri', {
      headers,
      method: 'DELETE',
      body: '',
    });
  });

  test('should proxy target service successful response', async () => {
    await createMockedAccessToken('access-token');
    createMockedResponse(200, 'Response body', {
      'content-type': 'text/plain',
      'x-request-id': 'request-id',
    });

    const request = new NextRequest('http://localhost/api/test-uri');
    const response = await route.GET(request);

    expect(response.status).toStrictEqual(200);
    expect(response.headers.get('content-type')).toStrictEqual('text/plain');
    expect(response.headers.get('x-request-id')).toStrictEqual('request-id');
    expect(await response.text()).toStrictEqual('Response body');

    const headers = new Headers(request.headers);
    headers.set('Authorization', 'Bearer access-token');

    expect(fetch).toHaveBeenCalledWith('https://api.konfigyr.com/test-uri', {
      headers,
      method: 'GET',
      body: null,
    });
  });

  test('should proxy request body to target service', async () => {
    await createMockedAccessToken('access-token');
    createMockedResponse(201, 'Created');

    const request = new NextRequest('http://localhost/api/test-uri', {
      method: 'POST',
      body: 'request body',
    });

    const response = await route.POST(request);

    expect(response.status).toStrictEqual(201);
    expect(await response.text()).toStrictEqual('Created');

    const headers = new Headers(request.headers);
    headers.set('Authorization', 'Bearer access-token');

    expect(fetch).toHaveBeenCalledWith('https://api.konfigyr.com/test-uri', {
      headers,
      method: 'POST',
      body: 'request body',
    });
  });

  test('should sanitize request headers before they are sent to target service', async () => {
    await createMockedAccessToken('access-token');
    createMockedResponse(204, null);

    const request = new NextRequest('http://localhost/api/test-uri', {
      method: 'PUT',
      body: 'request body',
      headers: {
        'x-request-id': 'request-id',
        'content-type': 'application/json',
        'host': 'ignore.me',
        'cookie': 'ignored',
      },
    });

    const response = await route.PUT(request);

    expect(response.status).toStrictEqual(204);
    expect(await response.text()).toStrictEqual('');

    const headers = new Headers();
    headers.set('Content-Type', 'application/json');
    headers.set('x-request-id', 'request-id');
    headers.set('Authorization', 'Bearer access-token');

    expect(fetch).toHaveBeenCalledWith('https://api.konfigyr.com/test-uri', {
      headers,
      method: 'PUT',
      body: 'request body',
    });
  });

  [502, 503, 504].forEach((status) => {
    test(`should respond with ${status} unavailable status code when target service is unavailable`, async () => {
      await createMockedAccessToken('access-token');
      createMockedResponse(status, 'Unavailable');

      const request = new NextRequest('http://localhost/api/test-uri', { method: 'PATCH' });
      const response = await route.PATCH(request);

      expect(response.status).toStrictEqual(status);
      expect(await response.json()).toMatchObject({
        status,
        title: 'Unavailable',
        detail: 'The server is currently unavailable, please try again later.',
        instance: '/api/test-uri',
      });

      expect(fetch).toHaveBeenCalled();
    });
  });

});
