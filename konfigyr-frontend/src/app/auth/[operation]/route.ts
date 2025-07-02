'use server';

import type { TokenEndpointResponse } from 'openid-client';
import type { Authentication, Token } from 'konfigyr/services/authentication';
import { NextRequest, NextResponse } from 'next/server';
import * as oauth from 'oauth4webapi';
import OpenidClient, { ErrorCodes, AuthorizationState, OAuthError } from 'konfigyr/services/openid';
import { setAuthentication } from 'konfigyr/services/authentication';
import * as session from 'konfigyr/services/session';

const STATE_KEY = 'oauth-authorization-state';
const ATTEMPTED_REQUEST_KEY = 'attempted-request';

interface Parameters {
  params: Promise<{ operation: string }>
}

const client = new OpenidClient(
  process.env.KONFIGYR_ID_URL || 'https://id.konfigyr.com',
  process.env.KONFIGYR_OAUTH_CLIENT_ID || 'konfigyr',
  process.env.KONFIGYR_OAUTH_CLIENT_SECRET || '',
);

/**
 * OAuth authorization operation handler function. It would attempt to redirect the current user
 * to the authorization server for authentication and store the authorization state.
 *
 * @param {NextRequest} request incoming HTTP request
 * @return {Promise<NextResponse>} redirect response to authorization server
 */
async function authorize(request: NextRequest): Promise<NextResponse> {
  const redirect = new URL('/auth/code', request.url);

  let authorization;

  try {
    authorization = await client.authorize(redirect.toString());
  } catch (ex) {
    console.log('Failed to generate authorization instructions', ex);
    return error(request, ex);
  }

  const response = NextResponse.redirect(authorization.uri);
  await session.set(response.cookies, STATE_KEY, authorization.state);

  if (request.nextUrl.searchParams.has('uri')) {
    await session.set(response.cookies, ATTEMPTED_REQUEST_KEY, request.nextUrl.searchParams.get('uri'));
  }

  return response;
}

/**
 * OAuth exchange operation handler function. This is where the authorization server would return the
 * user after attempted authentication. This request should contain the authorization code that must
 * be exchanged for an OAuth Access Token.
 *
 * @param {NextRequest} request incoming HTTP request from authorization server
 * @return {Promise<NextResponse>} redirect response to previously saved URL or home page
 */
async function exchange(request: NextRequest): Promise<NextResponse> {
  const state: AuthorizationState | undefined = await session.get(request.cookies, STATE_KEY);

  if (!state) {
    return error(request, ErrorCodes.MISSING_AUTHORIZATION_STATE);
  }

  let result;

  try {
    result = await client.exchange(new URL(request.url), state);
  } catch (ex) {
    console.log('Failed to exchange authorization code', ex);
    return error(request, ex);
  }

  const claims = result.claims();

  if (!claims) {
    return error(request, ErrorCodes.INVALID_RESPONSE);
  }

  const attemptedRequestUri: string = await session.get(request.cookies, ATTEMPTED_REQUEST_KEY) || '/';
  const response = NextResponse.redirect(new URL(attemptedRequestUri, request.url));

  await setAuthentication(response.cookies, createAuthentication(result));
  await session.remove(response.cookies, STATE_KEY);

  return response;
}

/**
 * Creates the `Authentication` object that would be stored in the session from the OIDC Authorization Server
 * response.
 *
 * @param {TokenEndpointResponse} response OAuth token response
 * @return {Authentication} Konfigyr authentication
 */
function createAuthentication(response: TokenEndpointResponse): Authentication {
  const token: Token = {
    access: response.access_token,
    refresh: response.refresh_token,
    expiry: Number(response.expires_in),
  };

  const scopes = response.scope?.split(' ') || [];

  return { token, scopes };
}

/**
 * Create a redirect response to the error page that would display the caught error. The error information
 * passed to the page as request query parameters.
 *
 * @param {NextRequest} request the HTTP request to extract the current operation and URL
 * @param {Error | string | unknown} error the caught error
 * @return {NextResponse} redirect response
 */
function error(request: NextRequest, error: Error | string | unknown): NextResponse {
  const url = new URL('/auth/error', request.url);

  if (error instanceof String || typeof error === 'string') {
    copy(url.searchParams, { code: error as string });
  } else if (error instanceof oauth.UnsupportedOperationError) {
    copy(url.searchParams, { code: error.code });
  } else if (error instanceof oauth.OperationProcessingError) {
    copy(url.searchParams, { code: error.code });
  } else if (error instanceof oauth.AuthorizationResponseError) {
    copy(url.searchParams, error);
  } else if (error instanceof oauth.ResponseBodyError) {
    copy(url.searchParams, error);
  } else {
    copy(url.searchParams, { code: ErrorCodes.INTERNAL_SERVER_ERROR });
  }

  return NextResponse.redirect(url);
}

/**
 * Copies the OAuth error values from the caught error into the URL search parameters that
 * would be part of the OAuth error URL.
 *
 * @param params search parameters where the error values are added to
 * @param error  the caught OAuth error that contains the error code
 */
function copy(params: URLSearchParams, error?: OAuthError & { code?: string }) {
  params.set('code', error?.code || oauth.INVALID_REQUEST);

  if (error?.error) {
    params.set('error', error.error);
  }
  if (error?.error_description) {
    params.set('error_description', error.error_description);
  }
  if (error?.error_uri) {
    params.set('error_uri', error.error_uri);
  }
}

/**
 * Route handler that would perform an {@link Operation authentication operation}.
 *
 * @param {NextRequest} request incoming request
 * @param {Parameters} params parameters containing the operation type
 * @return {Promise<NextResponse>} the operation response, usually a redirect
 */
export async function GET(request: NextRequest, { params }: Parameters) {
  const { operation } = await params;

  switch (operation) {
    case 'authorize':
      return await authorize(request);
    case 'code':
      return await exchange(request);
    default:
      return error(request, ErrorCodes.MISSING_AUTHORIZATION_STATE);
  }
}
