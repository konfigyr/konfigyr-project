import ky, { isHTTPError } from 'ky';

import type { BeforeErrorState } from 'ky';

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
  errors?: Array<ValidationError>,
}

const adapt = ({ error, request }: BeforeErrorState): Error => {
  if (!isHTTPError(error)) {
    return error;
  }

  if (request.method === 'HEAD') {
    return error;
  }

  if (error.data && typeof error.data === 'object') {
    error.problem = error.data as ProblemDetail;
  }

  return error;
};

export function resolvePrefixUrl(): string {
  if (typeof process.env.KONFIGYR_HTTP_HOST !== 'undefined') {
    return process.env.KONFIGYR_HTTP_HOST;
  }
  if (typeof window !== 'undefined') {
    return window.location.origin;
  }
  return '/';
}

/**
 * Exports the `ky` HTTP client with a custom error response adaptor that would parse the error
 * response body as a `ProblemDetail` object.
 *
 * Please note that this client is meant to be used with the Konfigyr REST API proxy route.
 */
export default ky.extend({
  prefix: resolvePrefixUrl(),
  hooks: {
    beforeError: [
      adapt,
    ],
  },
});

declare module 'ky' {
  interface HTTPError {
    problem?: ProblemDetail;
  }
}
