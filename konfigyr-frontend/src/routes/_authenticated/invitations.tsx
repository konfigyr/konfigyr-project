import { useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/layout';
import { getAccountInvitations } from '@konfigyr/hooks';
import { AccountInvitations } from '@konfigyr/components/account/invitations';

export const Route = createFileRoute('/_authenticated/invitations')({
  loader: ({ context }) => context.queryClient.ensureQueryData(getAccountInvitations()),
  component: RouteComponent,
});

function RouteComponent() {
  const { data: invitations } = useSuspenseQuery(getAccountInvitations());

  return (
    <LayoutContent>
      <LayoutNavbar title="Invitations" />
      <div className="w-full space-y-6 px-4">
        <AccountInvitations invitations={invitations} />
      </div>
    </LayoutContent>
  );
}
