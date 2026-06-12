
import { z } from 'zod';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { MonitorCloudIcon } from 'lucide-react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useCreateNamespaceApplication, useNamespace } from '@konfigyr/hooks';
import { NamespaceApplicationForm } from '@konfigyr/components/namespace/applications/application-form';
import { ApplicationTypeForm } from '@konfigyr/components/namespace/applications/application-type-form';
import { ApplicationsBreadcrumbs } from '@konfigyr/components/namespace/applications/breadcrumbs';
import { CreateNamespaceApplicationLabel } from '@konfigyr/components/namespace/applications/messages';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';

import type { CreateNamespaceApplication, NamespaceApplicationType } from '@konfigyr/hooks/types';

const searchSchema = z.object({
  type: z.enum(['SERVICE_ACCOUNT', 'AGENT', 'WORKLOAD']).optional(),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/create',
)({
  validateSearch: searchSchema,
  component: RouteComponent,
});

function RouteComponent() {
  const { type } = Route.useSearch();
  const namespace = useNamespace();
  const navigate = useNavigate({ from: Route.fullPath });

  const { mutateAsync: createNamespaceApplication } = useCreateNamespaceApplication(namespace.slug);

  const onTypeSelect = async (selectedType: NamespaceApplicationType) => {
    await navigate({ search: { type: selectedType } });
  };

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
    <div className="mx-4 space-y-6">
      <ApplicationsBreadcrumbs namespace={namespace}>
        <CreateNamespaceApplicationLabel />
      </ApplicationsBreadcrumbs>

      <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
        {!type ? (
          <ApplicationTypeForm onSubmit={onTypeSelect} />
        ) : (
          <Card className="border">
            <CardHeader>
              <CardTitle className="flex flex-col items-center gap-6 my-2">
                <MonitorCloudIcon size={64} strokeWidth="1" className="text-secondary" />
                <p className="text-center text-lg lg:text-xl">
                  <FormattedMessage
                    defaultMessage="Configure your application"
                    description="Heading on the application creation form step"
                  />
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
              <NamespaceApplicationForm
                namespace={namespace}
                type={type}
                handleSubmit={onNamespaceApplicationCreate}
              />
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
