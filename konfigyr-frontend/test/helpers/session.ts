import { vi } from 'vitest';

import type {
  AccessToken,
  AuthenticationSession,
  AuthorizationState,
  SessionData,
} from '@konfigyr/lib/authentication';

/**
 * The mocked session state that is used by the `useSession` hook to provide the
 * Authentication service with the session data.
 */
const authenticationSession = vi.hoisted(() => {
  let data: SessionData = {};

  const state = {
    id: 'konfigyr.sid',
    update: (updates: SessionData) => {
      data = { ...data, ...updates };
    },
    clear: () => {
      data = {};
    },
  };

  Object.defineProperty(state, 'data', {
    get: () => data,
  });

  return state;
}) as AuthenticationSession;


/**
 * Mock the TanStack `useSession` server hook that would load the mocked session.
 */
vi.mock('@tanstack/react-start/server', async () => {
  const server = await vi.importActual('@tanstack/react-start/server');

  return {
    ...server,
    getRequestUrl: vi.fn(() => new URL('http://localhost')),
    useSession: vi.fn(() => authenticationSession),
  };
});

const updateSessionAccessToken = async (data?: Partial<AccessToken>) => {
  const token = {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    expiresAt: Date.now() + 1000 * 60 * 60,
    ...(data || {}),
  } as AccessToken;

  await authenticationSession.update({ token });
};

const updateAuthorizationState = async (data?: Partial<AuthorizationState>) => {
  const state = {
    id: 'state-id',
    verifier: 'verifier',
    ...(data || {}),
  } as AuthorizationState;

  await authenticationSession.update({ state });
};

export {
  authenticationSession,
  updateAuthorizationState,
  updateSessionAccessToken,
};
