import {
  Card,
  CardContent,
} from '@konfigyr/components/ui/card';
import {
  useEditNamespaceApplication,
  useGetNamespaceApplication,
  useNamespace,
} from '@konfigyr/hooks';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { NamespaceApplicationForm } from '@konfigyr/components/namespace/applications/application-form';
import { NamespaceApplicationDetails } from '@konfigyr/components/namespace/applications/application-details';
import {toast} from 'sonner';
import {FormattedMessage} from 'react-intl';
import type {
  namespaceApplicationSchema} from '@konfigyr/components/namespace/applications/application-form';
import type { z } from 'zod';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/$id',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const navigate = useNavigate();

  const { id }  = Route.useParams();

  const { data: application } = useGetNamespaceApplication(namespace.slug, id);

  const { mutateAsync: editNamespaceApplication } = useEditNamespaceApplication(namespace.slug, id);

  const onNamespaceApplicationUpdate = async (value: z.infer<typeof namespaceApplicationSchema> ) => {
    await editNamespaceApplication({
      ...value,
      scopes: value.scopes.join(' '),
      expiresAt: value.expiresAt ? new Date(value.expiresAt).toISOString() : undefined,
    });

    toast.success(<FormattedMessage
      defaultMessage="The {application} was successfully updated."
      values={{ application: application!.name }}
      description="Success message"
    />);
  };

  return (
    <div className="w-full space-y-6 px-4 mx-auto">

      { application && (
        <NamespaceApplicationDetails
          namespace={namespace}
          namespaceApplication={application}
          showActions={true}
        />
      )}

      <Card className="border">
        <CardContent>
          <NamespaceApplicationForm
            namespace={namespace}
            namespaceApplication={application}
            handleSubmit={onNamespaceApplicationUpdate}
          />
        </CardContent>
      </Card>
    </div>
  );
}
