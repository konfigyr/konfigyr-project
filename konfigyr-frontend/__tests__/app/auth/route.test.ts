// @vitest-environment node

import { afterEach, describe, expect, test, vi } from 'vitest';
import { NextRequest, NextResponse } from 'next/server';
import {
  AuthorizationResponseError,
  OperationProcessingError,
  UnsupportedOperationError,
  INVALID_REQUEST,
  INVALID_SERVER_METADATA,
} from 'oauth4webapi';
import * as session from 'konfigyr/services/session';
import { GET, Operation } from 'konfigyr/app/auth/[operation]/route';

const client = vi.hoisted(() => {
  return { authorize: vi.fn(), exchange: vi.fn() };
});

vi.mock(import('konfigyr/services/openid'), async (original) => {
  const MockClient = vi.fn().mockImplementation(() => client);
  const mod = await original();

  return { ...mod, default: MockClient };
});

vi.mock(import('konfigyr/services/session'), async (original) => {
  const mod = await original();
  return { ...mod, get: vi.fn() };
});

const invokeOperation = async (operation: string): Promise<NextResponse> => {
  const request = new NextRequest(`http://localhost/auth/${operation}`);
  const params = Promise.resolve({ operation: operation as Operation });

  return await GET(request, { params });
};

describe('app/auth/operations', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  test('should redirect to OIDC authorization URI and store authorization state in a session cookie', async () => {
    client.authorize.mockReturnValue({
      uri: 'https://oidc.com/oauth/authorize',
      state: { state: 'request-state', verifier: 'request-verifier' },
    });

    const response = await invokeOperation(Operation.AUTHORIZE);

    expect(response.status).toStrictEqual(307);
    expect(response.cookies.has('konfigyr.session')).toStrictEqual(true);
    expect(response.headers.get('location')).toStrictEqual('https://oidc.com/oauth/authorize');
  });

  test('should process OIDC redirect and exchange the authorization code for identity', async () => {
    vi.mocked(session.get).mockResolvedValueOnce({
      state: 'request-state', verifier: 'request-verifier',
    });

    client.exchange.mockReturnValue({
      claims: () => ({ sub: '123456789' }),
      access_token: 'access-token',
    });

    const response = await invokeOperation(Operation.EXCHANGE);

    expect(response.status).toStrictEqual(307);
    expect(response.cookies.has('konfigyr.access')).toStrictEqual(true);
    expect(response.headers.get('location')).toStrictEqual('http://localhost/');
  });

  test('should redirect to error page when operation is not supported', async () => {
    const response = await invokeOperation('unsupported-operation');

    expect(response.status).toStrictEqual(307);
    expect(response.cookies.has('konfigyr.session')).toStrictEqual(false);
    expect(response.headers.get('location')).toStrictEqual('http://localhost/auth/error?code=OAUTH_MISSING_AUTHORIZATION_STATE');
  });

  test('should redirect to error page when OIDC service could not generate redirect URI due to runtime error', async () => {
    client.authorize.mockRejectedValue('Unexpected runtime error');

    const response = await invokeOperation(Operation.AUTHORIZE);

    expect(response.status).toStrictEqual(307);
    expect(response.cookies.has('konfigyr.session')).toStrictEqual(false);
    expect(response.headers.get('location')).toStrictEqual('http://localhost/auth/error?code=Unexpected+runtime+error');
  });

  test('should redirect to error page when OIDC service could not generate redirect URI due to unsupported operation', async () => {
    client.authorize.mockRejectedValue(new UnsupportedOperationError(INVALID_SERVER_METADATA));

    const response = await invokeOperation(Operation.AUTHORIZE);

    expect(response.status).toStrictEqual(307);
    expect(response.headers.get('location')).toStrictEqual('http://localhost/auth/error?code=OAUTH_UNSUPPORTED_OPERATION');
  });

  test('should redirect to error page when OIDC service could not generate redirect URI due to unsupported operation', async () => {
    client.authorize.mockRejectedValue(new OperationProcessingError(INVALID_REQUEST));

    const response = await invokeOperation(Operation.AUTHORIZE);

    expect(response.status).toStrictEqual(307);
    expect(response.cookies.has('konfigyr.session')).toStrictEqual(false);
    expect(response.headers.get('location')).toStrictEqual('http://localhost/auth/error?code=OAUTH_INVALID_REQUEST');
  });

  test('should redirect to error page when authorization state is not present in the session', async () => {
    vi.mocked(session.get).mockResolvedValue(undefined);

    const response = await invokeOperation(Operation.EXCHANGE);

    expect(response.status).toStrictEqual(307);
    expect(response.cookies.has('konfigyr.session')).toStrictEqual(false);
    expect(response.headers.get('location')).toStrictEqual('http://localhost/auth/error?code=OAUTH_MISSING_AUTHORIZATION_STATE');
  });

  test('should redirect to error page when authorization code could not be exchanged', async () => {
    vi.mocked(session.get).mockResolvedValue({
      state: 'request-state', verifier: 'request-verifier',
    });

    client.exchange.mockRejectedValue(new AuthorizationResponseError('Failed to authorize', {
      cause: new URLSearchParams(),
    }));

    const response = await invokeOperation(Operation.EXCHANGE);

    expect(response.status).toStrictEqual(307);
    expect(response.cookies.has('konfigyr.session')).toStrictEqual(false);
    expect(response.headers.get('location')).toStrictEqual('http://localhost/auth/error?code=OAUTH_AUTHORIZATION_RESPONSE_ERROR');
  });

});
