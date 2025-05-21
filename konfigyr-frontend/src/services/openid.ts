import type { Configuration, TokenEndpointResponse, TokenEndpointResponseHelpers } from 'openid-client';
import * as oauth from 'oauth4webapi';
import {
  discovery,
  buildAuthorizationUrl,
  authorizationCodeGrant,
  ClientSecretBasic,
  randomState,
  randomPKCECodeVerifier,
  calculatePKCECodeChallenge,
  allowInsecureRequests,
} from 'openid-client';

const DEFAULT_SCOPES = 'openid namespaces';

interface ClientCredentials {
  id: string;
  secret: string;
}

/**
 * Interface that defines the OAuth error structure that Authorization server may return
 * either as query parameters or as a JSON response.
 *
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-11.4">OAuth Error Registry</a>
 */
export interface OAuthError {
  error?: string,
  error_description?: string,
  error_uri?: string,
}

/**
 * Interface that defines the state that should be stored in the session before the redirect
 * to Authorization Server is made that initializes the authorization.
 * <p>
 * This state is later is used to verify the parameters when Authorization Server redirects to the
 * supplied `redirect_uri` and to exchange the received authorization code for access tokens.
 */
export interface AuthorizationState {
  state: string;
  verifier: string;
}

/**
 * Error codes that are used by the OpenID client.
 */
export const ErrorCodes = {
  INTERNAL_SERVER_ERROR: 'OAUTH_INTERNAL_SERVER_ERROR',
  MISSING_AUTHORIZATION_STATE: 'OAUTH_MISSING_AUTHORIZATION_STATE',
  RESPONSE_BODY_ERROR: oauth.RESPONSE_BODY_ERROR,
  UNSUPPORTED_OPERATION: oauth.UNSUPPORTED_OPERATION,
  WWW_AUTHENTICATE_CHALLENGE: oauth.WWW_AUTHENTICATE_CHALLENGE,
  AUTHORIZATION_RESPONSE_ERROR: oauth.AUTHORIZATION_RESPONSE_ERROR,
  JWT_USERINFO_EXPECTED: oauth.JWT_USERINFO_EXPECTED,
  PARSE_ERROR: oauth.PARSE_ERROR,
  INVALID_RESPONSE: oauth.INVALID_RESPONSE,
  INVALID_REQUEST: oauth.INVALID_REQUEST,
  RESPONSE_IS_NOT_JSON: oauth.RESPONSE_IS_NOT_JSON,
  RESPONSE_IS_NOT_CONFORM: oauth.RESPONSE_IS_NOT_CONFORM,
  HTTP_REQUEST_FORBIDDEN: oauth.HTTP_REQUEST_FORBIDDEN,
  REQUEST_PROTOCOL_FORBIDDEN: oauth.REQUEST_PROTOCOL_FORBIDDEN,
  JWT_TIMESTAMP_CHECK: oauth.JWT_TIMESTAMP_CHECK,
  JWT_CLAIM_COMPARISON: oauth.JWT_CLAIM_COMPARISON,
  JSON_ATTRIBUTE_COMPARISON: oauth.JSON_ATTRIBUTE_COMPARISON,
  KEY_SELECTION: oauth.KEY_SELECTION,
  MISSING_SERVER_METADATA: oauth.MISSING_SERVER_METADATA,
  INVALID_SERVER_METADATA: oauth.INVALID_SERVER_METADATA,
};

/**
 * OpenID client used to obtain OAuth Access Token using the `client_credentials` authorization grant types.
 */
export default class OpenidClient {
  #issuer: URL;
  #configuration: Configuration | null = null;
  #credentials: ClientCredentials;

  /**
   * Creates a new instance of the OpenID client that can be used to obtain the OAuth and ID Access Tokens
   * from the OIDC Authorization server. To interact with the Authorization server the OAuth Client credentials
   * need to be provided.
   *
   * @param issuer the OIDC Authorization server issuer URI
   * @param clientId OAuth client id
   * @param clientSecret OAuth client secret
   */
  constructor(issuer: string, clientId: string, clientSecret: string) {
    this.#issuer = new URL(issuer);
    this.#credentials = { id: clientId, secret: clientSecret };
  }

  async configuration(): Promise<Configuration> {
    if (this.#configuration === null) {
      this.#configuration = await discovery(
        this.#issuer,
        this.#credentials.id,
        this.#credentials.secret,
        ClientSecretBasic(this.#credentials.secret),
        {
          execute: [allowInsecureRequests],
        },
      );
    }

    return this.#configuration;
  }

  /**
   * Creates the parameters that are required to initiate the OAuth Authorization flow. This method would
   * return the Authorization server URL where OAuth Authorization flow would be initiated and the
   * {@link AuthorizationState} that should be stored in the session as it is needed later when
   * authorization code is exchanged.
   *
   * @param {string} redirect the redirect URL where Authorization server would send the authorization code
   * @param {string} scope the OAuth scope that should be sent to the Authorization server
   * @return {Promise<{ uri: URL, state: AuthorizationState }>} the redirect URI and authorization state
   */
  async authorize(redirect: string, scope: string = DEFAULT_SCOPES): Promise<{ uri: URL, state: AuthorizationState }> {
    const configuration = await this.configuration();

    const state = {
      state: randomState(),
      verifier: randomPKCECodeVerifier(),
    };

    const parameters: Record<string, string> = {
      scope,
      redirect_uri: redirect,
      code_challenge_method: 'S256',
      code_challenge: await calculatePKCECodeChallenge(state.verifier),
      ...state,
    };

    return { state, uri: buildAuthorizationUrl(configuration, parameters) };
  }

  /**
   * Validates the OAuth Authorization redirected request URL against the stored state. When state validation
   * is successful, the service would attempt to exchange the received authorization code for the OAuth
   * Access Tokens.
   *
   * @param {URL} url request URL containing the authorization code or errors
   * @param {AuthorizationState} state stored authorization state for validation
   * @return {Promise<TokenEndpointResponse>} the OAuth Access tokens response
   */
  async exchange(url: URL, state: AuthorizationState): Promise<TokenEndpointResponse & TokenEndpointResponseHelpers> {
    const configuration = await this.configuration();

    return await authorizationCodeGrant(configuration, url, {
      pkceCodeVerifier: state.verifier,
      expectedState: state.state,
    });
  }
}
