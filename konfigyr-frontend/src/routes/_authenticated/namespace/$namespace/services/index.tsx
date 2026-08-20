import { createFileRoute } from '@tanstack/react-router';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { NamespaceServices } from '@konfigyr/components/namespace/service/services';
import { useNamespace } from '@konfigyr/hooks/namespace/context';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();

  return (
    <LayoutContent>
      <LayoutNavbar title="Vault" />

      <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto mb-6">
        <NamespaceServices namespace={namespace} />
      </div>
    </LayoutContent>
  );
}
