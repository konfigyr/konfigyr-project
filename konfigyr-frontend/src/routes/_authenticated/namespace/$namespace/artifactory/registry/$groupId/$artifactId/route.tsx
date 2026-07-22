import { Outlet, createFileRoute } from '@tanstack/react-router';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { RegistryLabel } from '@konfigyr/components/artifactory/registry/messages';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/registry/$groupId/$artifactId',
)({
  component: RouteComponent,
});

function RouteComponent () {
  return (
    <LayoutContent>
      <LayoutNavbar title={( <RegistryLabel /> )} />
      <div className="mx-4 space-y-6">
        <Outlet />
      </div>
    </LayoutContent>
  );
}
