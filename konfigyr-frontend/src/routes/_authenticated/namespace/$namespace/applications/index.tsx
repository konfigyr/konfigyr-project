import { useNamespace } from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';
import { NamespaceApplications } from '@konfigyr/components/namespace/applications/applications';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  return (

    <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
      <NamespaceApplications namespace={namespace}/>
    </div>
  );
}

