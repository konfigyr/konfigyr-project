import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/components/layout';
import { Invitations } from '@konfigyr/components/namespace/members/invitations';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/invitations',
)({
  component: RouteComponent,
});

function RouteComponent() {
  return (
    <LayoutContent>
      <LayoutNavbar title="Invitations"/>

      <Invitations />
    </LayoutContent>
  );
}
