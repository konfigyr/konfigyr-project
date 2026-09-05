import { LayoutContent, LayoutNavbar } from '@konfigyr/layout';
import { Outlet, createFileRoute } from '@tanstack/react-router';
import { NamespaceApplicationTitle } from '@konfigyr/components/namespace/applications/messages';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/_overview/applications',
)({
  component: NamespaceApplicationsLayoutComponent,
});

function NamespaceApplicationsLayoutComponent() {
  return (
    <LayoutContent>
      <LayoutNavbar title={<NamespaceApplicationTitle />} />
      <Outlet />
    </LayoutContent>
  );
}
