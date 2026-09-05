'use server';

import Authentication from '@konfigyr/lib/authentication';
import { createLogger } from '@konfigyr/logger';
import { redirect } from '@tanstack/react-router';

const logger = createLogger('oauth/code');

export interface AuthorizationError {
  error: string | null,
  error_description: string | null,
  error_uri: string | null,
  retry_uri: string | null,
}

export default async function completeAuthorizationHandler (url: URL): Promise<AuthorizationError> {
  const authentication = await Authentication.get();
  let redirectUri: URL;

  try {
    redirectUri = await authentication.completeAuthorization(url);
  } catch (ex) {
    const state = authentication.authorizationState;
    logger.error({ stack: ex }, 'Failed to exchange authorization code');

    let error: string | null = url.searchParams.get('error');

    if (error === null && ex instanceof Error) {
      error = ex.message;
    }

    return {
      error,
      error_description: url.searchParams.get('error_description'),
      error_uri: url.searchParams.get('error_uri'),
      retry_uri: state?.uri ?? null,
    };
  }

  throw redirect({ href: redirectUri.href });
}
