import { Outlet, createFileRoute } from '@tanstack/react-router';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { GroupClaimsLabel } from '@konfigyr/components/groups/messages';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/groups/$groupId',
)({
  component: RouteComponent,
});

function RouteComponent () {
  return (
    <LayoutContent>
      <LayoutNavbar title={( <GroupClaimsLabel /> )} />
      <div className="mx-4 space-y-6">
        <Outlet />
      </div>
    </LayoutContent>
  );
}
