import { NextRequest, NextResponse } from 'next/server';
import csp, { CSP_HEADER_NAME } from 'konfigyr/middleware/csp';

export function middleware(request: NextRequest): NextResponse {
  const contentSecurityPolicy = csp(request);

  const response = NextResponse.next({
    request: {
      headers: request.headers,
    },
  });

  response.headers.set(CSP_HEADER_NAME, contentSecurityPolicy);

  return response;
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    {
      source: '/((?!api|_next/static|_next/image|favicon.ico).*)',
      missing: [
        { type: 'header', key: 'next-router-prefetch' },
        { type: 'header', key: 'purpose', value: 'prefetch' },
      ],
    },
  ],
};
