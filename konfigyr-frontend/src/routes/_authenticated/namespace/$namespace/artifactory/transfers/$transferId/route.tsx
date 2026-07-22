import { Outlet, createFileRoute } from '@tanstack/react-router';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { TransfersLabel } from '@konfigyr/components/artifactory/transfers/messages';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/transfers/$transferId',
)({
  component: RouteComponent,
});

function RouteComponent () {
  return (
    <LayoutContent>
      <LayoutNavbar title={( <TransfersLabel /> )} />
      <div className="mx-4 space-y-6">
        <Outlet />
      </div>
    </LayoutContent>
  );
}
