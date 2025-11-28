import { createStart } from '@tanstack/react-start';
import { authenticationMiddleware, securityPoliciesMiddleware } from '@konfigyr/middleware';

export const startInstance = createStart(() => ({
  defaultSsr: false,
  requestMiddleware: [
    authenticationMiddleware(),
    securityPoliciesMiddleware(),
  ],
}));
