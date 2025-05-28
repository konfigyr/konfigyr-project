'use server';

import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { clearAuthentication } from 'konfigyr/services/authentication';

/**
 * Logout action that can be used to clear the current `Authentication` session.
 *
 * When session is successfully cleared, the user is redirected to the home page with
 * `logout` query parameter set.
 */
export async function logout() {
  await clearAuthentication(await cookies());
  redirect('/?logout');
}
