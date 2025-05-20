import { NextResponse } from 'next/server';

export const PROBLEM_DETAIL_CONTENT_TYPE = 'application/problem+json';

/**
 * Interface that defines the structure of a validation error that would be present in the
 * `ProblemDetail` error response type.
 */
export interface ValidationError extends Record<string, unknown> {
  detail?: string,
  pointer?: string,
}

/**
 * Representation for an RFC 9457 problem detail that is being used by the Konfigyr REST API
 * to structure error responses.
 *
 * @see <a target="_blank" href="https://datatracker.ietf.org/doc/html/rfc9457">RFC 9457</a>
 */
export interface ProblemDetail extends Record<string, unknown> {
  type?: string,
  title?: string
  detail?: string,
  instance?: string,
  status?: number,
  errors?: ValidationError[],
}

/**
 * Error type that is used when an HTTP request receives a non-ok response from the target server.
 */
export class HttpResponseError extends Error {
  /**
   * The original HTTP response that was obtained from the target server.
   */
  response: Response;

  /**
   * The HTTP response body wrapped into a `ProblemDetail` instance.
   */
  problem: ProblemDetail;

  constructor(response: Response, problem: ProblemDetail) {
    super('Error occurred while executing request against: ' + response.url);
    this.response = response;
    this.problem = problem;
  }

  /**
   * The HTTP error response status code.
   */
  get status(): number {
    return this.response.status;
  }
}

/**
 * Executes the HTTP request using `fetch` function and wraps any HTTP error responses into an `HttpResponseError`.
 *
 * @param {string | URL | Request} request HTTP request instructions
 * @param {RequestInit} options HTTP request options
 * @return {Promise} promise that when resolved would contain the HTTP response JSON object or, when request fails, the `HttpResponseError`.
 */
export default async function request<T>(request: string | URL | Request, options?: RequestInit): Promise<T> {
  const response = await fetch(request, options);

  if (response.status === 200) {
    return await response.json();
  }

  throw new HttpResponseError(response, await response.json());
}

/**
 * Creates a {@link NextResponse} from the given problem detail.
 *
 * @param problem problem detail to be converted into a response
 */
export function createErrorResponse(problem: ProblemDetail): NextResponse {
  return NextResponse.json({
    type: 'about:blank',
    status: 500,
    ...problem,
  }, {
    status: problem.status || 500,
    headers: {
      'content-type': PROBLEM_DETAIL_CONTENT_TYPE,
    },
  });
}
