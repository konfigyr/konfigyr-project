import { createFileRoute, redirect } from '@tanstack/react-router';
import { getAccountInvitations, getAccountQuery, getLastUsedNamespace, getNamespacesQuery } from '@konfigyr/hooks';

/**
 * The index route that would attempt to resolve the last used namespace slug for the currently logged-in
 * user account. If the user never visited this application before on this user agent, it would redirect the
 * user to the namespace of the first available membership.
 *
 * In a scenario where the user account is not yet a member of any namespace, it would check for pending
 * account invitations. A single pending invitation redirects the user directly to its join page, multiple
 * pending invitations redirect to the invitations list, and no pending invitations redirect to the namespace
 * provisioning page.
 */
export const Route = createFileRoute('/_authenticated/')({
  loader: async ({ context }) => {
    const account = await context.queryClient.ensureQueryData(getAccountQuery());
    const namespaces = await context.queryClient.ensureQueryData(getNamespacesQuery());

    if (namespaces.length === 0) {
      const invitations = await context.queryClient.ensureQueryData(getAccountInvitations());
      const pending = invitations.filter(invitation => !invitation.expired);

      if (pending.length === 1) {
        throw redirect({ to: '/join/$key', params: { key: pending[0].key } });
      }

      if (pending.length > 1) {
        throw redirect({ to: '/invitations' });
      }

      throw redirect({ to: '/namespace/provision' });
    }

    let namespace = getLastUsedNamespace(account, namespaces);

    if (namespace == null) {
      namespace = namespaces[0].slug;
    }

    throw redirect({ to: '/namespace/$namespace', params: { namespace } });
  },
  preload: false,
});
