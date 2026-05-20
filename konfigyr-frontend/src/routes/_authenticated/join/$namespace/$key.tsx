import { useCallback } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import {
  getNamespaceInvitation,
  getNamespaceQuery,
  useJoinNamespace,
} from '@konfigyr/hooks';
import { JoinNamespace } from '@konfigyr/components/namespace/members/join';

export const Route = createFileRoute('/_authenticated/join/$namespace/$key')({
  component: RouteComponent,
  loader: async ({ context, params }) => {
    const namespace = await context.queryClient.ensureQueryData(getNamespaceQuery(params.namespace));
    const invitation = await context.queryClient.ensureQueryData(getNamespaceInvitation(namespace, params.key));

    return { namespace, invitation };
  },
});

function RouteComponent() {
  const navigate = Route.useNavigate();
  const { namespace, invitation } = Route.useLoaderData();
  const { mutateAsync: joinNamespace } = useJoinNamespace(namespace, invitation.key);

  const onAccept = useCallback(async () => {
    await joinNamespace();
    await navigate({ to: '/namespace/$namespace', params: { namespace: namespace.slug } });
  }, [namespace]);

  return (
    <div className="h-screen flex items-center justify-center bg-neutral-50">
      <div className="w-full lg:w-1/3 xl:w1/5 mx-auto p-4">
        <JoinNamespace
          namespace={namespace}
          invitation={invitation}
          onAccept={onAccept}
        />
      </div>
    </div>
  );
}
