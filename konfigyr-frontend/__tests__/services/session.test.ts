/* eslint-disable @typescript-eslint/no-explicit-any */
import { afterEach, describe, expect, test } from 'vitest';
import * as session from 'konfigyr/services/session';

export class Cookies implements session.CookieStore {
  #store: Map<string, { name: string, value: string }> = new Map();

  get(name: string): { name: string, value: string } | undefined {
    return this.#store.get(name);
  }

  set(options: any): void;
  set(name: string, value: string): void;
  set(name: string | any, value?: string): void {
    this.#store.set(name, { name, value: value! });
  }

  clear() {
    this.#store.clear();
  }
}

describe('services | session', () => {
  const cookies = new Cookies();

  afterEach(() => {
    cookies.clear();
  });

  test('should return an empty session value from cookie store when not known', async () => {
    expect(await session.get(cookies, 'key')).toBeUndefined();
  });

  test('should write and read session value from the cookie store', async () => {
    await session.set(cookies, 'key', 'new test value');

    expect(await session.get(cookies, 'key')).toStrictEqual('new test value');
  });

  test('should remove session value from cookie store', async () => {
    await session.set(cookies, 'key', 'test value');
    expect(await session.get(cookies, 'key')).toStrictEqual('test value');

    await session.remove(cookies, 'key');
    expect(await session.get(cookies, 'key')).toBeUndefined();
  });

  test('should fail to read from cookie store when using different secret key', async () => {
    const service = new session.SessionService('konfigyr.session');

    await session.set(cookies, 'key', 'test value');

    expect(await session.get(cookies, 'key')).toStrictEqual('test value');
    expect(await service.get(cookies, 'key')).toBeUndefined();
  });

  test('should clear cookie store', async () => {
    const service = new session.SessionService('cookie-name');

    await service.set(cookies, 'key', 'test value');
    expect(await service.get(cookies, 'key')).toStrictEqual('test value');
    expect(cookies.get('cookie-name')?.value).not.toHaveLength(0);

    await service.clear(cookies);
    expect(await service.get(cookies, 'key')).toBeUndefined();
    expect(cookies.get('cookie-name')?.value).toHaveLength(0);
  });

});
