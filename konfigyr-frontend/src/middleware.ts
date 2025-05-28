import { NextRequest, NextResponse } from 'next/server';
import csp, { CSP_HEADER_NAME } from 'konfigyr/middleware/csp';
import identified from 'konfigyr/middleware/authentication';

export async function middleware(request: NextRequest): Promise<NextResponse> {
  let response = await identified(request);

  if (response != null) {
    return response;
  }

  const contentSecurityPolicy = csp(request);

  response = NextResponse.next({
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
