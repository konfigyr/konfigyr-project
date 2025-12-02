import Authentication from '@konfigyr/lib/authentication';
import http from '@konfigyr/lib/http';

/**
 * Handler that is used by the account settings route to delete the user account.
 *
 * This method would execute a DELETE request to the Konfigyr API server to delete the user account.
 * If this request is successful, the user is logged out, the authentication session would be cleared,
 * and the response with a status code of 204 would be returned to the client.
 *
 * In case the request fails, or the user is not authenticated, an error would be thrown with the JSON
 * response body that contains a problem detail object describing the error that occurred.
 *
 * @returns {Promise<Response>} response with status code of 204 if the account deletion was successful.
 */
export async function onDeleteAccount() {
  const authentication = await Authentication.get();

  if (!authentication.authenticated) {
    return new Response(JSON.stringify({
      status: 401,
      title: 'Session expired',
      detail: 'Please login again to delete your account.',
    }), {
      status: 401,
      headers: {
        'content-type': 'application/json',
      },
    });
  }

  const request = authentication.authorizeRequest(
    new Request(
      new URL('/account', process.env.KONFIGYR_API_URL),
      { method: 'DELETE' },
    ),
  );

  await http(request).json();
  await authentication.logout();

  return new Response(null, { status: 204 });
}
