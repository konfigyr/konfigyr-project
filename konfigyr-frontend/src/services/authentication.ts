import request from './http';
import { SessionService, CookieStore } from './session';

const ACCESS_TOKEN_SESSION_KEY = 'access-token';
const ACCESS_TOKEN_DEFAULT_TTL = 28800; // 8 hours in seconds

const session = new SessionService('konfigyr.access', process.env.KONFIGYR_IDENTITY_SESSION_SECRET);

export interface Account {
  id: string,
  email: string,
  firstName?: string,
  lastName?: string,
  fullName?: string,
  avatar?: string,
  memberships?: [],
  deletable?: boolean,
  lastLoginAt?: string,
  createdAt?: string,
  updatedAt?: string,
}

export interface Token {
  access: string,
  refresh?: string,
  expiry?: number,
}

export interface Authentication {
  token: Token,
  scopes: string[],
}

/**
 * Error type that is used when no `Authentication` is present in the cookie/session store.
 */
export class AuthenticationNotFoundError extends Error {
  constructor() {
    super('Authentication is not present in the session store');
  }
}

/**
 * Retrieves the currently logged-in user account, from the cookie store.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 * @return {Promise<Account>} promise that once resolved would contain the account
 * @throws AuthenticationNotFoundError when `Token` is not present in the cookie store
 */
export async function getAccount(store: CookieStore): Promise<Account> {
  const token = await getToken(store);

  if (!token) {
    throw new AuthenticationNotFoundError();
  }

  const headers = new Headers();
  headers.set('authorization', `Bearer ${token.access}`);
  headers.set('content-type', 'application/json');

  return request<Account>(
    new URL('/account', process.env.KONFIGYR_API_URL || 'https://api.konfigyr.com'),
    { headers },
  );
}

/**
 * Retrieves the OAuth Tokens from the currently stored authentication.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 * @return {Promise<Token>} promise that once resolved would contain the tokens
 */
export async function getToken(store: CookieStore): Promise<Token | undefined> {
  const authentication = await getAuthentication(store);
  return authentication?.token;
}

/**
 * Retrieves the `Authentication` object, from the HTTP request cookie store.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 * @return {Promise<Authentication>} promise that once resolved would contain the authentication
 */
export async function getAuthentication(store: CookieStore): Promise<Authentication | undefined> {
  return await session.get(store, ACCESS_TOKEN_SESSION_KEY);
}

/**
 * Checks if `Authentication` object is present in the HTTP request cookie store.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 * @return {Promise<boolean>} promise that once resolved would return the identity state check result
 */
export async function isAuthenticated(store: CookieStore): Promise<boolean> {
  return getAuthentication(store).then(Boolean);
}

/**
 * Stores the `Authentication` in the HTTP cookie store.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 * @param {Authentication} authentication authentication to be stored
 */
export async function setAuthentication(store: CookieStore, authentication: Authentication) {
  const ttl = authentication.token.expiry || ACCESS_TOKEN_DEFAULT_TTL;
  await session.set(store, ACCESS_TOKEN_SESSION_KEY, authentication, ttl);
}

/**
 * Clears the `Authentication` from the HTTP cookie store.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 */
export async function clearAuthentication(store: CookieStore) {
  await session.set(store, ACCESS_TOKEN_SESSION_KEY, null);
}
