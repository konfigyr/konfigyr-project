import { createMiddleware } from '@tanstack/react-start';
import { setResponseHeaders } from '@tanstack/react-start/server';
import { generateNonce, setNonce } from '@konfigyr/lib/nonces';

import type { AnyRequestMiddleware } from '@tanstack/react-start';

interface Directives {
  [key: string]: Array<string>;
}

export const generateContentSecurityPolicies = (environment?: string, nonce?: string) => {
  const directives: Directives = {
    'base-uri': ['\'self\''],
    'default-src': ['\'self\''],
    'frame-ancestors': ['\'none\''],
    'font-src': ['\'self\'', 'data:'],
    'form-action': ['\'self\''],
    'frame-src': ['\'self\''],
    'connect-src': ['\'self\''],
    'img-src': ['\'self\'', 'blob:', 'data:', 'https:'],
    'manifest-src': ['\'self\''],
    'object-src': ['\'none\''],
    'script-src':['\'self\''],
    'style-src': ['\'self\'', '\'unsafe-inline\''],
    'style-src-elem': ['\'self\'', '\'unsafe-inline\''],
  };

  if (environment === 'development') {
    // Webpack use eval() in development mode for automatic JS reloading
    directives['script-src'].push('\'unsafe-inline\'');
    directives['script-src'].push('\'unsafe-eval\'');
  } else if (nonce) {
    // apply the generated nonce to the scripts when given
    directives['script-src'].push(`'nonce-${nonce}'`);
    directives['script-src'].push('\'strict-dynamic\'');
  }

  let secure: boolean = environment === 'production';

  if (process.env.KONFIGYR_SSL_ENABLED) {
    secure = /^(true|t|1)$/i.test(process.env.KONFIGYR_SSL_ENABLED);
  }

  if (secure) {
    directives['upgrade-insecure-requests'] = [];
  }

  return Object.entries(directives)
    .map(([key, value]) => `${key} ${value.join(' ')}`)
    .join('; ');
};

export function securityPoliciesMiddleware(): AnyRequestMiddleware {
  return createMiddleware({ type: 'request' })
    .server(({ next }) => {
      const environment = process.env.NODE_ENV || 'production';

      let nonce: string | undefined;
      if (environment === 'production') {
        nonce = generateNonce();
        setNonce(nonce);
      }

      const csp = generateContentSecurityPolicies(environment, nonce);

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
