import { createMiddleware } from '@tanstack/react-start';
import { setResponseHeaders } from '@tanstack/react-start/server';

import type { AnyRequestMiddleware } from '@tanstack/react-start';

const DEFAULT_DIRECTIVES = {
  'base-uri': ['\'self\''],
  'default-src': ['\'self\''],
  'frame-ancestors': ['\'none\''],
  'font-src': ['\'self\''],
  'form-action': ['\'self\''],
  'frame-src': ['\'self\''],
  'connect-src': ['\'self\''],
  'img-src': ['\'self\'', 'blob:', 'data:', 'https:'],
  'manifest-src': ['\'self\''],
  'object-src': ['\'none\''],
  'script-src': ['\'strict-dynamic\''],
  'style-src': ['\'self\''],
  'upgrade-insecure-requests': [],
};

export const generateContentSecurityPolicies = (environment?: string) => {
  const directives = {
    ...DEFAULT_DIRECTIVES,
    'script-src': [
      '\'self\'',
      '\'strict-dynamic\'',
    ],
    'style-src': [
      '\'self\'',
    ],
    'style-src-elem': [
      '\'self\'',
      '\'unsafe-inline\'',
    ],
  };

  if (environment === 'development') {
    // Webpack use eval() in development mode for automatic JS reloading
    directives['script-src'].push('\'unsafe-eval\'');
  }

  return Object.entries(directives)
    .map(([key, value]) => `${key} ${value.join(' ')}`)
    .join('; ');
};

export function securityPoliciesMiddleware(): AnyRequestMiddleware {
  const csp = generateContentSecurityPolicies(process.env.NODE_ENV);

  return createMiddleware({ type: 'request' })
    .server(({ next }) => {
      const headers = new Headers({
        'Content-Security-Policy': csp,
        'Cross-Origin-Opener-Policy': 'same-origin',
        'Cross-Origin-Resource-Policy': 'cross-origin',
        'Permissions-Policy': 'geolocation=(), camera=(), microphone=()',
        'Referrer-Policy': 'strict-origin-when-cross-origin',
        'X-Frame-Options': 'DENY',
        'X-Content-Type-Options': 'nosniff',
      });

      setResponseHeaders(headers);

      return next();
    });
}
