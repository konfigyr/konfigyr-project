'use server';

import { redirect } from 'next/navigation';
import { cookies } from 'next/headers';
import { getToken, clearAuthentication, AuthenticationNotFoundError } from 'konfigyr/services/authentication';
import request, { HttpResponseError, type ProblemDetail } from 'konfigyr/services/http';

export interface ActionState {
  error?: ProblemDetail,
}

export type AccountAction =
  | ((state: ActionState) => Promise<ActionState>)
  | ((state: ActionState, data: FormData) => Promise<ActionState>);

const createProblemDetail = (error: unknown) => {
  if (error instanceof HttpResponseError) {
    return error.problem;
  }

  if (error instanceof Error) {
    return { detail: error.message };
  }

  return { detail: 'Unexpected error occurred' };
};

/**
 * Server action that would delete the currently logged-in user account from the system. Once the
 * account is successfully removed we need to clear the authentication from the session and redirect
 * the user to home page.
 *
 * @param state form state that is passed by the delete account form.
 */
export async function deleteAccount(state: ActionState): Promise<ActionState> {
  const token = await getToken(await cookies());

  if (!token) {
    throw new AuthenticationNotFoundError();
  }

  const headers = new Headers();
  headers.set('content-type', 'application/json');
  headers.set('Authorization', `Bearer ${token.access}`);

  const url = new URL('/account', process.env.KONFIGYR_API_URL || 'https://api.konfigyr.com');

  try {
    await request(url, { method: 'DELETE', headers });
  } catch (error) {
    return { ...state, error: createProblemDetail(error) };
  }

  await cookies().then(clearAuthentication);

  return redirect('/');
}
