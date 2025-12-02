import { createFileRoute, redirect } from '@tanstack/react-router';
import { getAccountQuery, getLastUsedNamespace } from '@konfigyr/hooks';

/**
 * The index route that would attempt to resolve the last used namespace slug for the currently logged-in
 * user account. If the user never visited this application before on this user agent, it would redirect the
 * user to the namespace of the first available membership.
 *
 * In a scenario where the user account is not yet a member of any namespace, it would redirect the user to
 * the namespace provisioning page.
 */
export const Route = createFileRoute('/_authenticated/')({
  loader: async ({ context }) => {
    const account = await context.queryClient.ensureQueryData(getAccountQuery());

    if (account.memberships.length === 0) {
      throw redirect({ to: '/namespace/provision' });
    }

    let namespace = getLastUsedNamespace(account);

    if (namespace == null) {
      namespace = account.memberships[0].namespace;
    }

    throw redirect({ to: '/namespace/$namespace', params: { namespace } });
  },
  preload: false,
});
