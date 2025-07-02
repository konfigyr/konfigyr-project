import {assert, afterEach, describe, expect, test, vi, Mock} from 'vitest';
import * as authentication from 'konfigyr/services/authentication';
import { Cookies } from './session.test';

vi.mock('next/headers', () => ({
  cookies: () => Promise.resolve(),
}));

const createAuthentication = (): authentication.Authentication => ({
  token: { access: 'access-token' },
  scopes: ['openid'],
});

describe('services | authentication', () => {
  const cookies = new Cookies();

  afterEach(() => {
    cookies.clear();
    vi.resetAllMocks();
  });

  test('should return an empty Authentication value from request', async () => {
    const result = await authentication.getAuthentication(cookies);

    expect(result).toBeUndefined();
  });

  test('should return an empty Token value from request', async () => {
    const result = await authentication.getToken(cookies);

    expect(result).toBeUndefined();
  });

  test('should throw AuthenticationNotFound error when when session is not present in the request', async () => {
    await expect(authentication.getAccount(cookies)).rejects.toThrowError(authentication.AuthenticationNotFoundError);
  });

  test('should check if Authentication is present in the request', async () => {
    const result = await authentication.isAuthenticated(cookies);

    expect(result).toStrictEqual(false);
  });

  test('should store and read Authentication from the session store', async () => {
    const value = createAuthentication();
    await authentication.setAuthentication(cookies, value);

    const cookie = cookies.get('konfigyr.access');
    assert.isString(cookie?.value, 'Cookie value must be an encrypted string');

    expect(await authentication.isAuthenticated(cookies)).toStrictEqual(true);
    expect(await authentication.getAuthentication(cookies)).toStrictEqual(value);
  });

  test('should read Account from REST API using Token from the session store', async () => {
    const account = { id: 'test-account', email: 'john.doe@konfigyr.com' };

    global.fetch = vi.fn(() => Promise.resolve({
      status: 200,
      json: () => Promise.resolve(account),
    })) as Mock;

    const value = createAuthentication();
    await authentication.setAuthentication(cookies, value);

    expect(await authentication.getAccount(cookies)).toStrictEqual(account);
  });

  test('should read Token from the session store', async () => {
    expect(await authentication.getToken(cookies)).toBeUndefined();

    const value = createAuthentication();
    await authentication.setAuthentication(cookies, value);

    expect(await authentication.getToken(cookies)).toStrictEqual(value.token);
  });
});
