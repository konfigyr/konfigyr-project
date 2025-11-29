import { createLogger } from '@konfigyr/logger';
import Authentication from '@konfigyr/lib/authentication';
import { PROBLEM_DETAIL_CONTENT_TYPE } from '@konfigyr/lib/http';

import type { ProblemDetail } from '@konfigyr/lib/http';

const IGNORED_HEADERS = ['cookie', 'host'];

const logger = createLogger('services/api-proxy');

const createProxyTargetUrl = (original: URL) => {
  const target = new URL(process.env.KONFIGYR_API_URL || 'http://localhost:8080');
  target.pathname = original.pathname.replace(/^\/api/, '');
  target.search = original.search;
  return target;
};

function copyHeaders(headers: Headers): Headers {
  const target = new Headers();

  headers.forEach((value, name) => {
    if (!IGNORED_HEADERS.includes(name)) {
      target.append(name, value);
    }
  });

  return target;
}

async function copyBody(request: Request): Promise<string | null> {
  switch (request.method.toLowerCase()) {
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

const renderErrorResponse = (problem: ProblemDetail) => {
  return new Response(JSON.stringify({
    type: 'about:blank',
    status: 500,
    ...problem,
  }), {
    status: problem.status || 500,
    headers: {
      'content-type': PROBLEM_DETAIL_CONTENT_TYPE,
    },
  });
};

/**
 * Proxies the given request to the Konfigyr API server. The request is authorized using the `Authorization`
 * service, or an `AuthenticationError` is thrown if the user is not authenticated.
 * <p>
 * The target URL is constructed by stripping any `/api` path prefix from the original request URL.
 * <p>
 * The following headers are ignored from the original request:
 *  - `cookie`
 *  - `host`
 *
 * @param request the request to be proxied
 * @returns the response from the Konfigyr API server
 * @throws {AuthenticationError} if the user is not authenticated
 */
export async function proxy(request: Request): Promise<Response> {
  const authentication = await Authentication.get();

  if (!authentication.authenticated) {
    return renderErrorResponse({
      status: 401,
      title: 'Not authenticated',
      detail: 'You need to login to access this resource.',
    });
  }

  const outgoing = new Request(
    createProxyTargetUrl(new URL(request.url)),
    {
      credentials: 'omit',
      method: request.method,
      headers: copyHeaders(request.headers),
      body: await copyBody(request),
    },
  );

  await authentication.authorizeRequest(outgoing);

  logger.debug(`Proxying request to: '${outgoing.method} ${outgoing.url}'`);

  let response: Response;

  try {
    response = await fetch(outgoing);
  } catch (ex) {
    return renderErrorResponse({
      title: 'Internal Server Error',
      detail: 'An unexpected error occurred while proxying the request.',
      status: 500,
    });
  }

  logger.debug(`Obtained proxy response for request '${request.method} ${outgoing.url}' with status ${response.status}`);

  if (isProxyUnavailable(response)) {
    return renderErrorResponse({
      title: 'Unavailable',
      detail: 'The server is currently unavailable, please try again later.',
      status: response.status,
    });
  }

  return response;
}
