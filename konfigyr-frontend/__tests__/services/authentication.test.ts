import { assert, afterEach, describe, expect, test, vi } from 'vitest';
import * as authentication from 'konfigyr/services/authentication';
import { Cookies } from './session.test';

vi.mock('next/headers', () => ({
  cookies: () => Promise.resolve(),
}));

const createAuthentication = (): authentication.Authentication => ({
  account: {
    oid: 'test-account',
    email: 'paul.atreides@arakis.com',
    name: 'Paul Atreides',
    picture: 'https://images.com/paul.png',
  },
  token: { access: 'access-token' },
  scopes: ['openid'],
});

describe('services | identity', () => {
  const cookies = new Cookies();

  afterEach(() => {
    cookies.clear();
    vi.resetAllMocks();
  });

  test('should return an empty Authentication value from request', async () => {
    const result = await authentication.getAuthentication(cookies);

    expect(result).toBeUndefined();
  });

  test('should return an empty Account value from request', async () => {
    const result = await authentication.getAccount(cookies);

    expect(result).toBeUndefined();
  });

  test('should return an empty Token value from request', async () => {
    const result = await authentication.getToken(cookies);

    expect(result).toBeUndefined();
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

  test('should read Account from the session store', async () => {
    expect(await authentication.getAccount(cookies)).toBeUndefined();

    const value = createAuthentication();
    await authentication.setAuthentication(cookies, value);

    expect(await authentication.getAccount(cookies)).toStrictEqual(value.account);
  });

  test('should read Token from the session store', async () => {
    expect(await authentication.getToken(cookies)).toBeUndefined();

    const value = createAuthentication();
    await authentication.setAuthentication(cookies, value);

    expect(await authentication.getToken(cookies)).toStrictEqual(value.token);
  });
});
