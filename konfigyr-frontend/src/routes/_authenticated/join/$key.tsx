import { useCallback } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { getAccountInvitation, useAcceptInvitation } from '@konfigyr/hooks';
import { JoinNamespace } from '@konfigyr/components/namespace/members/join';

export const Route = createFileRoute('/_authenticated/join/$key')({
  component: RouteComponent,
  loader: async ({ context, params }) => await context.queryClient.ensureQueryData(getAccountInvitation(params.key)),
});

function RouteComponent() {
  const navigate = Route.useNavigate();
  const invitation = Route.useLoaderData();
  const { mutateAsync: acceptInvitation } = useAcceptInvitation();

  const onAccept = useCallback(async () => {
    await acceptInvitation(invitation.key);
    await navigate({ to: '/namespace/$namespace', params: { namespace: invitation.organization.slug } });
  }, [invitation]);

  return (
    <div className="h-screen flex items-center justify-center bg-neutral-50">
      <div className="w-full lg:w-1/3 xl:w1/5 mx-auto p-4">
        <JoinNamespace
          invitation={invitation}
          onAccept={onAccept}
        />
      </div>
    </div>
  );
}
