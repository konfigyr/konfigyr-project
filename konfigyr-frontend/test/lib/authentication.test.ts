// @vitest-environment node

import { afterEach, describe, expect, test, vi } from 'vitest';
import Authentication, { AuthenticationError } from '@konfigyr/lib/authentication';
import {
  updateAuthorizationState,
  updateSessionAccessToken,
} from '@konfigyr/test/helpers/session';

describe('services | authentication', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetAllMocks();
  });

  test('should fail to create an Authentication without secret', async () => {
    vi.stubEnv('KONFIGYR_SESSION_KEY', undefined);

    await expect(Authentication.get()).rejects.toThrow('KONFIGYR_SESSION_KEY environment variable is not set');
  });

  test('should create an Authentication without session data', async () => {
    const authentication = await Authentication.get();

    expect(authentication).toBeInstanceOf(Authentication);
    expect(authentication.authenticated).toBe(false);
    expect(authentication.expired).toBe(true);
    expect(authentication.accessToken).toEqual(null);
    expect(authentication.authorizationState).toEqual(null);
  });

  test('should start and complete authorization process', async () => {
    const authentication = await Authentication.get();

    const authorizationUri = await authentication.startAuthorization('http://localhost/attempted-uri');

    const state = authentication.authorizationState;

    expect(state).toBeDefined();
    expect(state?.id).toBeDefined();
    expect(state?.verifier).toBeDefined();
    expect(state?.uri).toEqual('http://localhost/attempted-uri');

    expect(authorizationUri.origin).toEqual('https://id.konfigyr.com');
    expect(authorizationUri.pathname).toEqual('/oauth/authorize');
    expect(authorizationUri.searchParams.get('client_id')).toEqual('konfigyr');
    expect(authorizationUri.searchParams.get('response_type')).toEqual('code');
    expect(authorizationUri.searchParams.get('scope')).toEqual('openid namespaces profiles');
    expect(authorizationUri.searchParams.get('redirect_uri')).toEqual('http://localhost/auth/code');
    expect(authorizationUri.searchParams.get('code_challenge_method')).toEqual('S256');
    expect(authorizationUri.searchParams.get('state')).toEqual(state!.id);

    const result = await authentication.completeAuthorization(new URL(
      `http://localhost/auth/code?code=1234567890&state=${state!.id}`,
    ));

    expect(result).toEqual(URL.parse(state!.uri));
    expect(authentication.authenticated).toBe(true);
    expect(authentication.expired).toBe(false);
    expect(authentication.accessToken).toMatchObject({
      accessToken: 'access-token-jwt',
      refreshToken: 'refresh-token',
    });
  });

  test('should obtain new access token using the refresh token', async () => {
    const authentication = await Authentication.get();

    await updateSessionAccessToken({ refreshToken: 'refresh-token-to-be-consumed' });

    await authentication.refresh();
    expect(authentication.accessToken).toMatchObject({
      accessToken: 'access-token-jwt',
      refreshToken: 'refresh-token',
    });
  });

  test('should not be able to refresh the authentication without session', async () => {
    const authentication = await Authentication.get();

    await authentication.refresh();

    expect(authentication.accessToken).toEqual(null);
  });

  test('should not be able to complete authorization without state', async () => {
    const authentication = await Authentication.get();

    expect(authentication.completeAuthorization(new URL('http://localhost/auth/code')))
      .rejects.toThrow('Authorization state is not present in the session');
  });

  test('should authorize request when authenticated', async () => {
    const authentication = await Authentication.get();
    const request = new Request('http://localhost/some-uri');

    await updateSessionAccessToken();

    expect(authentication.authorizeRequest(request)).toBe(request);

    expect(request.headers.get('Authorization')).toEqual('Bearer access-token');
  });

  test('should fail to authorize request when not authenticated', async () => {
    const authentication = await Authentication.get();
    const request = new Request('http://localhost/some-uri');

    expect(() => authentication.authorizeRequest(request))
      .toThrow(AuthenticationError);

    expect(request.headers.get('Authorization')).toEqual(null);
  });

  test('should reset authentication state by clearing the access token', async () => {
    const authentication = await Authentication.get();

    await updateAuthorizationState({ id: 'state-id' });
    await updateSessionAccessToken({ accessToken: 'access-token' });

    expect(authentication.accessToken).toMatchObject({ accessToken: 'access-token' });
    expect(authentication.authorizationState).toMatchObject({ id: 'state-id' });

    await authentication.reset();

    expect(authentication.accessToken).toEqual(null);
    expect(authentication.authorizationState).toMatchObject({ id: 'state-id' });
  });

  test('should logout the current user', async () => {
    const authentication = await Authentication.get();

    await updateAuthorizationState({ id: 'state-id' });
    await updateSessionAccessToken({ accessToken: 'access-token' });

    expect(authentication.accessToken).toMatchObject({ accessToken: 'access-token' });
    expect(authentication.authorizationState).toMatchObject({ id: 'state-id' });

    await authentication.logout();

    expect(authentication.accessToken).toEqual(null);
    expect(authentication.authorizationState).toEqual(null);
  });
});
