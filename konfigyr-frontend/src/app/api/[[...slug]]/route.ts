import { NextRequest, NextResponse } from 'next/server';
import { createErrorResponse } from 'konfigyr/services/http';
import * as tokens from 'konfigyr/services/identity';

const TARGET = process.env.KONFIGYR_API_URL || 'http://localhost:8080';
const IGNORED_HEADERS = ['cookie', 'host'];

function copyHeaders(headers: Headers, token: string): Headers {
  const target = new Headers();

  headers.forEach((value, name) => {
    if (!IGNORED_HEADERS.includes(name)) {
      target.append(name, value);
    }
  });

  target.set('Authorization', `Bearer ${token}`);

  return target;
}

async function copyBody(request: NextRequest, method: string): Promise<string | null> {
  switch (method?.toLowerCase()) {
    case 'get':
    case 'head':
    case 'options':
      return null;
    default:
      return await request.text();
  }
}

function isProxyUnavailable(response: Response): boolean {
  switch (response.status) {
    case 502:
    case 503:
    case 504:
      return true;
    default:
      return false;
  }
}

/**
 * Executor function that would proxy the {@link NextRequest} to Konfigyr REST API and append the
 * OAuth Access Token as a Bearer Authentication Token.
 *
 * @param request request to be proxied
 * @return the proxied response
 */
async function execute(request: NextRequest): Promise<NextResponse> {
  const token = await tokens.get(request);

  if (!token) {
    return createErrorResponse({
      title: 'Not authenticated',
      detail: 'You need to login to access this resource',
      instance: request.nextUrl.pathname,
      status: 401,
    });
  }

  const url = TARGET + request.nextUrl.pathname.replace('/api', '');

  const response = await fetch(url, {
    method: request.method,
    headers: copyHeaders(request.headers, token.token),
    body: await copyBody(request, request.method),
  });

  if (isProxyUnavailable(response)) {
    return createErrorResponse({
      title: 'Unavailable',
      detail: 'The server is currently unavailable, please try again later.',
      instance: request.nextUrl.pathname,
      status: response.status,
    });
  }

  return new NextResponse(await response.text(), {
    status: response.status,
    headers: response.headers,
  });
}

async function proxy(request: NextRequest): Promise<NextResponse> {
  try {
    return await execute(request);
  } catch (ex) {
    console.warn('Unexpected error occurred while proxying request: %s', request.url, ex);

    return createErrorResponse({
      title: 'Internal server error',
      detail: 'Unexpected error occurred while processing your request',
      instance: request.nextUrl.pathname,
      status: 500,
    });
  }
}

export async function GET(request: NextRequest) {
  return await proxy(request);
}

export async function POST(request: NextRequest) {
  return await proxy(request);
}

export async function PUT(request: NextRequest) {
  return await proxy(request);
}

export async function PATCH(request: NextRequest) {
  return await proxy(request);
}

export async function DELETE(request: NextRequest) {
  return await proxy(request);
}
