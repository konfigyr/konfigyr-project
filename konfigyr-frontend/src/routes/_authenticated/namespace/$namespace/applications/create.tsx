import {
  Card,
  CardContent,
} from '@konfigyr/components/ui/card';
import { useCreateNamespaceApplication, useNamespace } from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';
import { NamespaceApplicationForm } from '@konfigyr/components/namespace/applications/application-form';
import {useState} from 'react';
import {NamespaceApplicationDetails} from '@konfigyr/components/namespace/applications/application-details';
import type {NamespaceApplication} from '@konfigyr/hooks/namespace/types';
import type { z } from 'zod';
import type {namespaceApplicationSchema } from '@konfigyr/components/namespace/applications/application-form';


export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/create',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const [application, setApplication] = useState<NamespaceApplication>();

  const { mutateAsync: createNamespaceApplication } = useCreateNamespaceApplication(namespace.slug);

  const onNamespaceApplicationCreate = async (value: z.infer<typeof namespaceApplicationSchema> ) => {
    const created = await createNamespaceApplication({
      ...value,
      scopes: value.scopes.join(' '),
      expiresAt: value.expiresAt ? new Date(value.expiresAt).toISOString() : undefined,
    });
    setApplication(created);
  };

  return (
    <div className="w-full space-y-6 px-4 mx-auto">
      <Card className="border">
        <CardContent>
          { application ?
            <NamespaceApplicationDetails namespace={namespace} namespaceApplication={application} />
            :
            <NamespaceApplicationForm namespace={namespace} handleSubmit={onNamespaceApplicationCreate} />
          }
        </CardContent>
      </Card>
    </div>
  );
}
