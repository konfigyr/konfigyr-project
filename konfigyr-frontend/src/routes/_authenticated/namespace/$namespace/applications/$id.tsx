import { toast } from 'sonner';
import { MonitorCloud } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { createFileRoute, useLocation } from '@tanstack/react-router';
import {
  Card,
  CardContent,
} from '@konfigyr/components/ui/card';
import {
  useEditNamespaceApplication,
  useGetNamespaceApplication,
  useNamespace,
} from '@konfigyr/hooks';
import { NamespaceApplicationForm } from '@konfigyr/components/namespace/applications/application-form';
import { ApplicationDetails } from '@konfigyr/components/namespace/applications/application-details';
import { ErrorState } from '@konfigyr/components/error';
import { EmptyState } from '@konfigyr/components/ui/empty';

import type { CreateNamespaceApplication } from '@konfigyr/hooks/types';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/$id',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const { id }  = Route.useParams();
  const namespace = useNamespace();

  const location = useLocation();
  const state = location.state as { clientSecret?: string };

  const { data: application, error, isPending, isError } = useGetNamespaceApplication(namespace.slug, id);
  const { mutateAsync: editNamespaceApplication } = useEditNamespaceApplication(namespace.slug, id);

  const onNamespaceApplicationUpdate = async (value: CreateNamespaceApplication) => {
    await editNamespaceApplication(value);

    toast.success(<FormattedMessage
      defaultMessage="The {application} was successfully updated."
      values={{ application: application!.name }}
      description="Success message"
    />);
  };

  return (
    <div className="lg:w-2/3 xl:w-3/5 space-y-6  mx-auto">
      {isPending && (
        <article data-slot="namespace-application-skeleton">
          <EmptyState
            title="Namespase application"
            description="Namespace application is loading. Please wait"
            icon={<MonitorCloud />}
          />
        </article>
      )}

      {isError && (
        <ErrorState error={error} className="border-none" />
      )}

      {application && (
        <>
          <ApplicationDetails
            namespace={namespace}
            application={{
              ...application,
              clientSecret: state.clientSecret,
            }}
          />

          <Card className="border">
            <CardContent>
              <NamespaceApplicationForm
                namespace={namespace}
                namespaceApplication={application}
                handleSubmit={onNamespaceApplicationUpdate}
              />
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
