import {
  ClientSecretBasic,
  allowInsecureRequests,
  authorizationCodeGrant,
  buildAuthorizationUrl,
  calculatePKCECodeChallenge,
  discovery,
  randomPKCECodeVerifier,
  randomState,
  refreshTokenGrant,
} from 'openid-client';
import { useSession } from '@tanstack/react-start/server';
import { createLogger } from '@konfigyr/logger';

import type {
  Configuration,
  TokenEndpointResponse,
  TokenEndpointResponseHelpers,
} from 'openid-client';

const logger = createLogger('services/authentication');

const DEFAULT_SCOPES = 'openid namespaces profiles';

export class AuthenticationError extends Error {

}

export interface AccessToken {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

export interface AuthorizationState {
  id: string;
  uri: string;
  verifier: string;
}

export interface SessionData {
  token?: AccessToken;
  state?: AuthorizationState;
}

export interface AuthenticationSession {
  readonly id: string | undefined;
  readonly data: SessionData;
  update: (update: SessionData) => Promise<AuthenticationSession>;
  clear: () => Promise<AuthenticationSession>;
}

const useAuthenticationSession = () => {
  const password = process.env.KONFIGYR_SESSION_KEY;

  if (typeof password !== 'string') {
    throw new Error('KONFIGYR_SESSION_KEY environment variable is not set');
  }

  return useSession<SessionData>({ name: 'konfigyr.sid', password }) as Promise<AuthenticationSession>;
};

const discover = () => {
  const issuer = new URL(process.env.KONFIGYR_ID_URL || 'https://id.konfigyr.com');

  logger.debug(`Discovering OIDC configuration metadata for Issuer URI ${issuer}`);

  return discovery(
    issuer,
    process.env.KONFIGYR_OAUTH_CLIENT_ID || 'konfigyr',
    process.env.KONFIGYR_OAUTH_CLIENT_SECRET,
    ClientSecretBasic(process.env.KONFIGYR_OAUTH_CLIENT_SECRET),
    {
      execute: [allowInsecureRequests],
    },
  );
};

const createAccessToken = (response: TokenEndpointResponse & TokenEndpointResponseHelpers): AccessToken => {
  const claims = response.claims() || {};

  logger.debug('Obtained OAuth Access Token response with ID Token claims: %j', claims);

  if (typeof response.refresh_token !== 'string') {
    throw new Error('OAuth2 Access Token response does not contain the "refresh_token" property');
  }

  if (typeof response.expires_in !== 'number') {
    throw new Error('OAuth2 Access Token response does not contain the "expires_in" property');
  }

  return {
    accessToken: response.access_token,
    refreshToken: response.refresh_token,
    expiresAt: Date.now() + (response.expires_in * 1000),
  };
};

export default class Authentication {
  static #configuration: Configuration | null = null;

  #session: AuthenticationSession;
  #metadata: Configuration;

  static async get(): Promise<Authentication> {
    const session = await useAuthenticationSession();

    if (Authentication.#configuration == null) {
      Authentication.#configuration = await discover();
    }

    return new Authentication(session, Authentication.#configuration);
  }

  private constructor(session: AuthenticationSession, metadata: Configuration) {
    this.#session = session;
    this.#metadata = metadata;
  }

  get authenticated(): boolean {
    return this.accessToken !== null;
  }

  /**
   * Checks if this session present and that the obtain OAuth2 Access Token is not expired.
   *
   * @returns {boolean} `true` if the session is expired, `false` otherwise.
   */
  get expired() {
    return this.accessToken === null || this.accessToken.expiresAt < Date.now();
  }

  /**
   * Extracts the current OAuth2 Access Token from the session. This property may be
   * `null` if the session is not present.
   *
   * @type {AccessToken | null}
   */
  get accessToken(): AccessToken | null {
    return this.#session.data.token || null;
  }

  /**
   * Extracts the current authorization state from the session. This property may be
   * `null` if the session is not present.
   *
   * @type {AuthorizationState | null}
   */
  get authorizationState(): AuthorizationState | null {
    return this.#session.data.state || null;
  }

  /**
   * Authorizes the given request by adding the `Authorization` header with the `Bearer` token
   * extracted from the current session.
   * <p>
   * If the token is not present in the session, or it is expired, an authentication error would
   * be thrown.
   *
   * @param request the request to be authorized
   * @returns the authorized request
   * @throws {AuthenticationError} if the session does not contain an active Access Token
   */
  authorizeRequest(request: Request): Request {
    if (!this.authenticated) {
      throw new AuthenticationError('Authentication session does not contain an active Access Token, cannot authorize request');
    }

    request.headers.set('authorization', `Bearer ${this.accessToken?.accessToken}`);

    return request;
  }

  async refresh(scope = DEFAULT_SCOPES): Promise<void> {
    const token = this.accessToken;

    if (token === null) {
      return;
    }

    logger.debug(`Attempting to refresh OAuth2 Access Token with [scope: ${scope}`);

    const response = await refreshTokenGrant(this.#metadata, token.refreshToken, { scope });
    const accessToken = createAccessToken(response);

    await this.#session.update({ state: undefined, token: accessToken });

    logger.info(`Successfully completed the OAuth2 Access Token refresh, token expires at: ${new Date(accessToken.expiresAt)}`);
  }

  async startAuthorization(uri: URL | string, scope = DEFAULT_SCOPES): Promise<URL> {
    const redirectUri = new URL('/auth/code', uri);

    logger.debug(`Attempting to start OAuth2 authorization with [callback_uri: ${uri}, scope: ${scope}, redirect_uri=${redirectUri}]`);

    const state = {
      uri: String(uri),
      id: randomState(),
      verifier: randomPKCECodeVerifier(),
    };

    const parameters: Record<string, string> = {
      scope,
      state: state.id,
      redirect_uri: String(redirectUri),
      code_challenge_method: 'S256',
      code_challenge: await calculatePKCECodeChallenge(state.verifier),
    };

    const authorizationUrl = buildAuthorizationUrl(this.#metadata, parameters);
    await this.#session.update({ ...this.#session.data, state });

    return authorizationUrl;
  }

  async completeAuthorization(uri: URL): Promise<URL> {
    const state = this.authorizationState;

    if (state === null) {
      throw new Error('Authorization state is not present in the session');
    }

    logger.debug(`Attempting to exchange the OAuth2 authorization code for Access Token for state: ${state.id}`);

    const response = await authorizationCodeGrant(this.#metadata, uri, {
      pkceCodeVerifier: state.verifier,
      expectedState: state.id,
    });

    const token: AccessToken = createAccessToken(response);
    await this.#session.update({ state: undefined, token });

    logger.info(`Successfully completed the OAuth2 Access Token exchange, token expires at: ${new Date(token.expiresAt)}`);

    return new URL(state.uri);
  }

  async reset() {
    logger.debug(`Resetting Authentication OAuth Access Token session with identifier: ${this.#session.id}`);

    await this.#session.update({ token: undefined });
  }

  async logout() {
    logger.debug(`Attempting to destroy Authentication session with identifier: ${this.#session.id}`);

    await this.#session.update({ state: undefined, token: undefined });
  }
}

