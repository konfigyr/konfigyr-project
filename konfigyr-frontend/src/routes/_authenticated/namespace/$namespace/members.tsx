import { useNamespace } from '@konfigyr/hooks';
import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/components/layout';
import { InviteFormCard } from '@konfigyr/components/namespace/members/invitation-form';
import { Members } from '@konfigyr/components/namespace/members/members';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/members',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();

  return (
    <LayoutContent>
      <LayoutNavbar title="Members"/>

      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <InviteFormCard namespace={namespace} />
        <Members namespace={namespace} />
      </div>
    </LayoutContent>
  );
}
