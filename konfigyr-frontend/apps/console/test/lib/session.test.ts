// @vitest-environment node

import { afterEach, describe, expect, test, vi } from 'vitest';
import { sealData, unsealData } from 'iron-session';
import { deleteCookie, setCookie } from '@tanstack/react-start/server';
import { createSession } from '@konfigyr/lib/session';
import { cookies } from '@konfigyr/test/helpers/session';

import type { SessionOptions } from '@konfigyr/lib/session';

interface TestSessionData {
  user?: string;
  theme?: string;
}

const options: SessionOptions<TestSessionData> = {
  name: 'test.sid',
  password: 'test-session-password-of-32-chars',
  fields: ['user', 'theme'],
  cookie: { path: '/', httpOnly: true },
};

const seal = (data: unknown) => sealData(data, { password: options.password });

const unseal = () => unsealData(cookies.get(options.name)!, { password: options.password });

describe('services | session', () => {
  afterEach(() => vi.clearAllMocks());

  test('should create an empty session when session cookie is not present', async () => {
    const session = await createSession(options);

    expect(session.data).toStrictEqual({});
  });

  test('should create a session from the sealed session cookie', async () => {
    cookies.set(options.name, await seal({ user: 'john.doe', theme: 'dark' }));

    const session = await createSession(options);

    expect(session.data).toStrictEqual({ user: 'john.doe', theme: 'dark' });
  });

  test('should only read configured fields from the session cookie', async () => {
    cookies.set(options.name, await seal({ user: 'john.doe', id: 'session-id', data: { theme: 'dark' } }));

    const session = await createSession(options);

    expect(session.data).toStrictEqual({ user: 'john.doe' });
  });

  test('should create an empty session when session cookie cannot be unsealed', async () => {
    cookies.set(options.name, 'Fe26.2*1*invalid-seal');

    const session = await createSession(options);

    expect(session.data).toStrictEqual({});
  });

  test('should create an empty session when session cookie is sealed with a different password', async () => {
    cookies.set(options.name, await sealData({ user: 'john.doe' }, { password: 'another-session-password-32-chars' }));

    const session = await createSession(options);

    expect(session.data).toStrictEqual({});
  });

  test('should merge updates and write them to the sealed session cookie', async () => {
    cookies.set(options.name, await seal({ user: 'john.doe' }));

    const session = await createSession(options);

    await expect(session.update({ theme: 'dark' })).resolves.toBe(session);
    expect(session.data).toStrictEqual({ user: 'john.doe', theme: 'dark' });

    await session.update({ user: undefined });
    expect(session.data).toStrictEqual({ user: undefined, theme: 'dark' });

    await expect(unseal()).resolves.toStrictEqual({ theme: 'dark' });
    expect(setCookie).toHaveBeenLastCalledWith(options.name, cookies.get(options.name), options.cookie);
  });

  test('should clear the session data and delete the session cookie', async () => {
    cookies.set(options.name, await seal({ user: 'john.doe' }));

    const session = await createSession(options);

    await expect(session.clear()).resolves.toBe(session);

    expect(session.data).toStrictEqual({});
    expect(cookies.has(options.name)).toBe(false);
    expect(deleteCookie).toHaveBeenCalledWith(options.name, options.cookie);
  });
});
