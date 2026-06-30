
import { useNamespace } from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/groups/',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  return (
    <LayoutContent>
      <LayoutNavbar title="Group claims"/>
      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
      </div>
    </LayoutContent>
  );
}

