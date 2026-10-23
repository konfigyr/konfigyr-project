import { createStart } from '@tanstack/react-start';
import { authenticationMiddleware, csrfMiddleware, securityPoliciesMiddleware } from '@konfigyr/middleware';

export const startInstance = createStart(() => ({
  defaultSsr: false,
  requestMiddleware: [
    csrfMiddleware(),
    securityPoliciesMiddleware(),
    authenticationMiddleware(),
  ],
}));
