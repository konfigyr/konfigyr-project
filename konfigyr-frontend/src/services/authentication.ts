import { SessionService, CookieStore } from './session';

const ACCESS_TOKEN_SESSION_KEY = 'access-token';

const session = new SessionService('konfigyr.access', process.env.KONFIGYR_IDENTITY_SESSION_SECRET);

export interface Account {
  oid: string,
  name: string,
  email: string,
  picture?: string,
}

export interface Token {
  access: string,
  refresh?: string,
  expiry?: number,
}

export interface Authentication {
  account: Account,
  token: Token,
  scopes: string[],
}

/**
 * Retrieves the currently logged-in user account, from the cookie store.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 * @return {Promise<Account>} promise that once resolved would contain the account
 */
export async function getAccount(store: CookieStore): Promise<Account | undefined> {
  const authentication = await getAuthentication(store);
  return authentication?.account;
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
  await session.set(store, ACCESS_TOKEN_SESSION_KEY, authentication);
}

/**
 * Clears the `Authentication` from the HTTP cookie store.
 *
 * @param {CookieStore} store store that has access to HTTP cookies
 */
export async function clearAuthentication(store: CookieStore) {
  await session.set(store, ACCESS_TOKEN_SESSION_KEY, null);
}
