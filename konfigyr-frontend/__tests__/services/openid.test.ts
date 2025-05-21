// @vitest-environment node

import { afterEach, describe, expect, Mock, test, vi} from 'vitest';
import OpenidClient from 'konfigyr/services/openid';

const mockIdentityProvider = () => {
  global.fetch = vi.fn((url) => {
    let body;

    if (url === 'https://oidc.com/.well-known/openid-configuration') {
      body = {
        issuer: 'https://oidc.com',
        authorization_endpoint: 'https://oidc.com/oauth/authorize',
        token_endpoint: 'https://oidc.com/oauth/token',
      };
    } else if (url === 'https://oidc.com/oauth/token') {
      body = {
        token_type: 'bearer',
        access_token: 'access token',
      };
    } else {
      body = null;
    }

    return Promise.resolve(new Response(JSON.stringify(body), { status: 200 }));
  }) as Mock;
};

describe('services | openid', () => {

  afterEach(() => {
    vi.resetAllMocks();
  });

  test('should fail to create client configuration with invalid OIDC issuer URI', () => {
    expect(() => new OpenidClient('invalid-issuer-uri', 'client-id', 'secret')).toThrowError(TypeError);
  });

  test('should create client configuration from OIDC issuer URI', async () => {
    mockIdentityProvider();

    const client = new OpenidClient('https://oidc.com', 'test-client-id', 'test-client-secret');
    const configuration = await client.configuration();

    expect(configuration.clientMetadata()).toMatchObject({
      client_id: 'test-client-id',
      client_secret: 'test-client-secret',
    });

    expect(configuration.serverMetadata()).toMatchObject({
      issuer: 'https://oidc.com',
    });

    expect(await client.configuration()).toEqual(configuration);

    expect(fetch).toHaveBeenCalledTimes(1);
  });

  test('should create OAuth2 authorization URL with state value', async () => {
    mockIdentityProvider();

    const client = new OpenidClient('https://oidc.com', 'test-client-id', 'test-client-secret');
    const { uri, state } = await client.authorize('https:/client.com/auth/code');

    expect(uri.host).toStrictEqual('oidc.com');
    expect(uri.pathname).toStrictEqual('/oauth/authorize');
    expect(uri.searchParams.get('client_id')).toStrictEqual('test-client-id');
    expect(uri.searchParams.get('response_type')).toStrictEqual('code');
    expect(uri.searchParams.get('redirect_uri')).toStrictEqual('https:/client.com/auth/code');
    expect(uri.searchParams.get('scope')).toStrictEqual('openid namespaces');
    expect(uri.searchParams.get('code_challenge_method')).toStrictEqual('S256');
    expect(uri.searchParams.has('code_challenge')).toBeTruthy();
    expect(uri.searchParams.get('verifier')).toStrictEqual(state.verifier);
    expect(uri.searchParams.get('state')).toStrictEqual(state.state);

    expect(fetch).toHaveBeenCalledTimes(1);
  });

  test('should exchange OAuth2 authorization code for access token', async () => {
    mockIdentityProvider();

    const client = new OpenidClient('https://oidc.com', 'test-client-id', 'test-client-secret');
    const token = await client.exchange(
      new URL('https:/client.com/auth/code?state=oauth-state&code=auth-code'),
      { verifier: 'verifier', state: 'oauth-state' },
    );

    expect(token).toMatchObject({
      token_type: 'bearer',
      access_token: 'access token',
    });

    expect(fetch).toHaveBeenCalledTimes(2);
  });

});
