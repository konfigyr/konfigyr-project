import { useCallback } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { getAccountInvitation, useAcceptInvitation, useDeclineInvitation } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { JoinNamespace } from '@konfigyr/components/account/join';

export const Route = createFileRoute('/_authenticated/join/$key')({
  component: RouteComponent,
  loader: async ({ context, params }) => await context.queryClient.ensureQueryData(getAccountInvitation(params.key)),
});

function RouteComponent() {
  const navigate = Route.useNavigate();
  const invitation = Route.useLoaderData();
  const errorNotification = useErrorNotification();
  const { mutateAsync: acceptInvitation } = useAcceptInvitation(invitation.key);
  const { mutateAsync: declineInvitation } = useDeclineInvitation(invitation.key);

  const onAccept = useCallback(async () => {
    try {
      await acceptInvitation();
    } catch (error) {
      return errorNotification(error);
    }

    return navigate({ to: '/namespace/$namespace', params: { namespace: invitation.organization.slug } });
  }, [invitation, acceptInvitation, errorNotification, navigate]);

  const onDecline = useCallback(async () => {
    try {
      await declineInvitation();
    } catch (error) {
      return errorNotification(error);
    }

    return navigate({ to: '/' });
  }, [declineInvitation, errorNotification, navigate]);

  return (
    <div className="h-screen flex items-center justify-center bg-neutral-50">
      <div className="w-full lg:w-1/3 xl:w1/5 mx-auto p-4">
        <JoinNamespace
          invitation={invitation}
          onAccept={onAccept}
          onDecline={onDecline}
        />
      </div>
    </div>
  );
}
