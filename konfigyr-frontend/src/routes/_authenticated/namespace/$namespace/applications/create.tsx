import {
  Card,
  CardContent,
} from '@konfigyr/components/ui/card';
import { useCreateNamespaceApplication, useNamespace } from '@konfigyr/hooks';
import {createFileRoute, useNavigate} from '@tanstack/react-router';
import { NamespaceApplicationForm } from '@konfigyr/components/namespace/applications/application-form';
import type { z } from 'zod';
import type {namespaceApplicationSchema } from '@konfigyr/components/namespace/applications/application-form';


export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/applications/create',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const navigate = useNavigate();

  const { mutateAsync: createNamespaceApplication } = useCreateNamespaceApplication(namespace.slug);

  const onNamespaceApplicationCreate = async (value: z.infer<typeof namespaceApplicationSchema> ) => {
    const created = await createNamespaceApplication({
      ...value,
      scopes: value.scopes.join(' '),
      expiresAt: value.expiresAt ? new Date(value.expiresAt).toISOString() : undefined,
    });
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
    <div className="w-full space-y-6 px-4 mx-auto">
      <Card className="border">
        <CardContent>
          <NamespaceApplicationForm namespace={namespace} handleSubmit={onNamespaceApplicationCreate} />
        </CardContent>
      </Card>
    </div>
  );
}
