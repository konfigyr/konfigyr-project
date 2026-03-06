import { FormattedMessage } from 'react-intl';
import { MonitorCloudIcon } from 'lucide-react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useCreateNamespaceApplication, useNamespace } from '@konfigyr/hooks';
import { NamespaceApplicationForm } from '@konfigyr/components/namespace/applications/application-form';
import { CreateNamespaceApplicationLabel } from '@konfigyr/components/namespace/applications/messages';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';

import type { CreateNamespaceApplication } from '@konfigyr/hooks/types';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/create',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const navigate = useNavigate();

  const { mutateAsync: createNamespaceApplication } = useCreateNamespaceApplication(namespace.slug);

  const onNamespaceApplicationCreate = async (value: CreateNamespaceApplication) => {
    const created = await createNamespaceApplication(value);

    await navigate({
      to: '/namespace/$namespace/applications/$id',
      params: { namespace: namespace.slug, id: created.id },
      state: (prev) => ({
        ...prev,
        clientSecret: created.clientSecret,
      }),
    });
  };

  return (
    <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex flex-col items-center gap-6 my-2">
            <MonitorCloudIcon size={64} strokeWidth="1" className="text-secondary" />
            <p className="text-center text-lg lg:text-xl">
              <CreateNamespaceApplicationLabel />
            </p>
          </CardTitle>
          <CardDescription>
            <FormattedMessage
              defaultMessage="Register an application to securely connect your services or CI/CD pipelines to the Konfigyr API. Each application creates an OAuth client with scoped permissions for operations like metadata upload and configuration access."
              description="Description text that is shown when user tries to create a new namespace application"
            />
          </CardDescription>
        </CardHeader>
        <CardContent>
          <NamespaceApplicationForm namespace={namespace} handleSubmit={onNamespaceApplicationCreate} />
        </CardContent>
      </Card>
    </div>
  );
}
