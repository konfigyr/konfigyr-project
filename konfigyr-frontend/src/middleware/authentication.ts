import type { NextRequest } from 'next/server';
import { NextResponse } from 'next/server';
import { isAuthenticated } from 'konfigyr/services/authentication';
import * as session from 'konfigyr/services/session';

const ATTEMPTED_REQUEST_KEY = 'attempted-request';

const ALLOWED_PATTERNS = [
  /^\/$/, // allow index page
  /^\/(auth|error)\/.+$/, // allows /auth/*, /error/*
];

function isAllowed(url: string): boolean {
  for (const pattern of ALLOWED_PATTERNS) {
    if (url.match(pattern)) {
      return true;
    }
  }

  return false;
}

export default async function identified(request: NextRequest): Promise<NextResponse | null> {
  const url = request.nextUrl.pathname;

  if (isAllowed(url)) {
    return null;
  }

  if (await isAuthenticated(request.cookies)) {
    return null;
  }

  const redirect = new URL('/auth/authorize', request.url);
  const response =  NextResponse.redirect(redirect);

  await session.set(response.cookies, ATTEMPTED_REQUEST_KEY, request.url);

  return response;
}
