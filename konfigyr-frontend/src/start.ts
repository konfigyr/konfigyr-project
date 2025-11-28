import { createStart } from '@tanstack/react-start';
import { securityPoliciesMiddleware } from '@konfigyr/middleware';

export const startInstance = createStart(() => ({
  defaultSsr: false,
  requestMiddleware: [
    securityPoliciesMiddleware(),
  ],
}));
