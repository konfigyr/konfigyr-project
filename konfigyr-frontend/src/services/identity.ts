import type { NextRequest, NextResponse } from 'next/server';
import { SessionService } from './session';

const ACCESS_TOKEN_SESSION_KEY = 'access-token';

const session = new SessionService('konfigyr.access', process.env.KONFIGYR_IDENTITY_SESSION_SECRET!);

export interface Identity {
  email: string,
  token: string,
}

/**
 * Retrieves the currently logged-in user account, or `Identity`, from the HTTP request cookie store.
 *
 * @param {NextRequest} request HTTP Next request
 * @return {Promise<Identity>} promise that once resolved would contain the identity
 */
export async function get(request: NextRequest): Promise<Identity | undefined> {
  return await session.get(request.cookies, ACCESS_TOKEN_SESSION_KEY);
}

/**
 * Checks if the currently logged-in user account, or `Identity`, is present in the HTTP request cookie store.
 *
 * @param {NextRequest} request HTTP Next request
 * @return {Promise<boolean>} promise that once resolved would return the identity state check result
 */
export async function has(request: NextRequest): Promise<boolean> {
  return get(request).then(Boolean);
}

/**
 * Stores the currently logged-in user account, or `Identity`, to HTTP response cookie store.
 *
 * @param {NextResponse} response HTTP Next response
 * @param {Identity} identity identity to be stored
 */
export async function set(response: NextResponse, identity: Identity) {
  await session.set(response.cookies, ACCESS_TOKEN_SESSION_KEY, identity);
}
