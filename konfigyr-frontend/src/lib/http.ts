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
