/* eslint-disable @stylistic/quotes */
import type { NextRequest } from 'next/server';

export const NONCE_HEADER_NAME = 'x-nonce';
export const CSP_HEADER_NAME = 'Content-Security-Policy';

const DEFAULT_DIRECTIVES = {
  'base-uri': [`'self'`],
  'default-src': [`'self'`],
  'frame-ancestors': [`'none'`],
  'font-src': [`'self'`],
  'form-action': [`'self'`],
  'frame-src': [`'self'`],
  'connect-src': [`'self'`],
  'img-src': [`'self'`, `blob:`, `data:`, `https:`],
  'manifest-src': [`'self'`],
  'object-src': [`'none'`],
  'script-src': [`'strict-dynamic'`],
  'style-src': [`'self'`],
  'upgrade-insecure-requests': [],
};

export function generateNonce() {
  return Buffer.from(crypto.randomUUID()).toString('base64');
}

function generateContentSecurityPolicies(nonce: string) {
  const directives = {
    ...DEFAULT_DIRECTIVES,
    'script-src': [
      `'self'`,
      `'nonce-${nonce}'`,
      `'strict-dynamic'`,
    ],
    'style-src': [
      `'self'`,
      `'nonce-${nonce}'`,
    ],
    'style-src-elem': [
      `'self'`,
      `'unsafe-inline'`,
    ],
  };

  if (process.env.NODE_ENV === 'development') {
    // Webpack use eval() in development mode for automatic JS reloading
    directives['script-src'].push(`'unsafe-eval'`);
  }

  return Object.entries(directives)
    .map(([key, value]) => `${key} ${value.join(' ')}`)
    .join('; ');
}

export default function csp(request: NextRequest, nonce: string = generateNonce()): string {
  const policies = generateContentSecurityPolicies(nonce);

  request.headers.set(NONCE_HEADER_NAME, nonce);
  request.headers.set(CSP_HEADER_NAME, policies);

  return policies;
}
