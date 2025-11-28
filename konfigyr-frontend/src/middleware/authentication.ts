import { createMiddleware} from '@tanstack/react-start';
import { redirect } from '@tanstack/react-router';
import { getRequestUrl } from '@tanstack/react-start/server';
import Authentication from '@konfigyr/lib/authentication';
import { createLogger } from '@konfigyr/logger';

import type { AnyRequestMiddleware } from '@tanstack/react-start';

const logger = createLogger('middleware/authentication');

const ALLOWED_PATTERNS = [
  /^\/(api|auth|error|favicon.ico)\/.+$/, // allows /api/*, /auth/*, /error/*, favicon.ico
];

const authorize = async (authentication: Authentication, url: URL) => {
  const authorizationUri = await authentication.startAuthorization(url);

  logger.debug(`Redirecting to OIDC Authorization Server with authorization URI: ${authorizationUri}`);

  throw redirect({ href: authorizationUri.href });
};

export function authenticationMiddleware(): AnyRequestMiddleware {
  return createMiddleware({ type: 'request' })
    .server(async ({ next }) => {
      const url = getRequestUrl();

      // check if the request path is allowed to be accessed without authentication
      for (const pattern of ALLOWED_PATTERNS) {
        if (url.pathname.match(pattern)) {
          return next();
        }
      }

      const authentication = await Authentication.get();

      if (authentication.expired) {
        try {
          await authentication.refresh();
        } catch (error) {
          await authentication.reset();

          logger.warn(error, 'Failed to refresh OAuth Access token, redirecting to OIDC Authorization Server');
          return await authorize(authentication, url);
        }
      }

      if (authentication.authenticated) {
        return next();
      }

      return await authorize(authentication, url);
    });
}
