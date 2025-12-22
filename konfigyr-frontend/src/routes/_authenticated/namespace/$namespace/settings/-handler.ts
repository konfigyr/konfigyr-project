import { z } from 'zod';
import Authentication from '@konfigyr/lib/authentication';
import http from '@konfigyr/lib/http';
import { redirect } from '@tanstack/react-router';

export const DeleteNamespaceSchema = z.object({
  namespace: z.string(),
  redirect: z.string().optional(),
});

export type DeleteNamespaceRequest = z.infer<typeof DeleteNamespaceSchema>;

/**
 * Handler that is used by the Namespace settings route to delete the selected namespace.
 *
 * This method would execute a DELETE request to the Konfigyr API server to delete the namespace.
 * If this request is successful, the user would be redirected to provided namespace, specified by the
 * `redirect` request parameter, or the namespace provisioning page if none is provided.
 *
 * In case the request fails, or the user is not authenticated, an error would be thrown with the JSON
 * response body that contains a problem detail object describing the error that occurred.
 *
 * @param data request body containing the namespace slug and optional redirect parameter.
 * @returns {Promise<Response>} response with status code of 302 if the namespace deletion was successful.
 */
export async function deleteNamespaceHandler({ data }: { data: DeleteNamespaceRequest }) {
  const authentication = await Authentication.get();

  if (!authentication.authenticated) {
    return new Response(JSON.stringify({
      status: 401,
      title: 'Session expired',
      detail: 'Please login again to delete your namespace.',
    }), {
      status: 401,
      headers: {
        'content-type': 'application/json',
      },
    });
  }

  const request = authentication.authorizeRequest(
    new Request(
      new URL(`/namespaces/${data.namespace}`, process.env.KONFIGYR_API_URL),
      { method: 'DELETE' },
    ),
  );

  await http(request).json();

  if (data.redirect) {
    throw redirect({ to: '/namespace/$namespace', params: { namespace: data.redirect } });
  }

  throw redirect({ to: '/namespace/provision' });
}
