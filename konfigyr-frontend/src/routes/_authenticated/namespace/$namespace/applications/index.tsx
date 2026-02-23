
import { PlusIcon } from 'lucide-react';
import { useNamespace } from '@konfigyr/hooks';
import { Button } from '@konfigyr/components/ui/button';
import { Link, createFileRoute } from '@tanstack/react-router';
import { NamespaceApplications } from '@konfigyr/components/namespace/applications/applications';
import { CreateNamespaceApplicationLabel } from '@konfigyr/components/namespace/applications/messages';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  return (

    <div className="w-full space-y-6 px-4">
      <div className="flex justify-end items-center gap-4">
        <Button variant="ghost" asChild>
          <Link
            to="/namespace/$namespace/applications/create"
            params={{ namespace: namespace.slug }}
          >
            <PlusIcon size="1rem"/>
            <CreateNamespaceApplicationLabel />
          </Link>
        </Button>
      </div>
      <NamespaceApplications namespace={namespace}/>
    </div>
  );
}

