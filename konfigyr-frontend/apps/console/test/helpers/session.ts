import { vi } from 'vitest';
import { sealData, unsealData } from 'iron-session';

import type {
  AccessToken,
  AuthorizationState,
  SessionData,
} from '@konfigyr/lib/authentication';

const SESSION_COOKIE_NAME = 'konfigyr.sid';

/**
 * The in-memory cookie jar that replaces the request and response cookies of the
 * TanStack Start server functions.
 */
const cookies = vi.hoisted(() => new Map<string, string>());

/**
 * Mock the TanStack Start server cookie functions that are used to read and write
 * the sealed authentication session cookie.
 */
vi.mock('@tanstack/react-start/server', async () => {
  const server = await vi.importActual('@tanstack/react-start/server');

  return {
    ...server,
    getRequestUrl: vi.fn(() => new URL('http://localhost')),
    getCookie: vi.fn((name: string) => cookies.get(name)),
    setCookie: vi.fn((name: string, value: string) => {
      cookies.set(name, value);
    }),
    deleteCookie: vi.fn((name: string) => {
      cookies.delete(name);
    }),
  };
});

const password = () => process.env.KONFIGYR_SESSION_KEY!;

/**
 * Reads the data stored in the sealed authentication session cookie.
 */
const readSessionData = async (): Promise<SessionData> => {
  const seal = cookies.get(SESSION_COOKIE_NAME);
  return seal ? unsealData<SessionData>(seal, { password: password() }) : {};
};

const updateSessionData = async (update: SessionData) => {
  const data = { ...await readSessionData(), ...update };
  cookies.set(SESSION_COOKIE_NAME, await sealData(data, { password: password() }));
};

/**
 * Removes all cookies from the in-memory cookie jar.
 */
const clearSession = () => cookies.clear();

const updateSessionAccessToken = async (data?: Partial<AccessToken>) => {
  const token: AccessToken = {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    expiresAt: Date.now() + 1000 * 60 * 60,
    ...(data || {}),
  };

  await updateSessionData({ token });
};

const updateAuthorizationState = async (data?: Partial<AuthorizationState>) => {
  const state = {
    id: 'state-id',
    verifier: 'verifier',
    ...(data || {}),
  } as AuthorizationState;

  await updateSessionData({ state });
};

export {
  SESSION_COOKIE_NAME,
  clearSession,
  cookies,
  readSessionData,
  updateAuthorizationState,
  updateSessionAccessToken,
};
