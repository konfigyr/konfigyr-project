import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { Outlet, createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/kms',
)({
  component: KmsLayoutComponent,
});

function KmsLayoutComponent() {
  return (
    <LayoutContent>
      <LayoutNavbar title="KMS" />
      <Outlet />
    </LayoutContent>
  );
}
