import { LayoutContent, LayoutNavbar } from '@konfigyr/layout';
import { Outlet, createFileRoute } from '@tanstack/react-router';
import {
  KeyManagementSystemModuleLabel,
} from '@konfigyr/components/messages/modules';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/kms',
)({
  component: KmsLayoutComponent,
});

function KmsLayoutComponent() {
  return (
    <LayoutContent>
      <LayoutNavbar title={( <KeyManagementSystemModuleLabel /> )} />
      <Outlet />
    </LayoutContent>
  );
}
