import { toast } from 'sonner';
import { useCallback  } from 'react';
import { FormattedMessage } from 'react-intl';
import { CircuitBoardIcon } from 'lucide-react';
import { LayoutContent } from '@konfigyr/components/layout';
import { CreateNamespaceForm } from '@konfigyr/components/namespace';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { createFileRoute, useNavigate } from '@tanstack/react-router';

import type { Namespace } from '@konfigyr/hooks/types';

/**
 * The namespace provisioning route where users can create new namespaces.
 */
export const Route = createFileRoute('/_authenticated/namespace/provision')({
  component: RouteComponent,
  head: () => ({
    meta: [{
      title: 'Create namespace | Konfigyr',
    }],
  }),
  preload: false,
});

function RouteComponent() {
  const navigate = useNavigate();

  const onNamespaceCreate = useCallback(async (namespace: Namespace) => {
    toast.success(<FormattedMessage
      defaultMessage="{name} was successfully created"
      values={{ name: namespace.name }}
      description="Notification message shown when namespace was successfully created"
    />);

    await navigate({ to: '/namespace/$namespace', params: { namespace: namespace.slug } });
  }, [navigate]);

  return (
    <LayoutContent variant="centered">
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex flex-col items-center gap-6 my-2">
            <CircuitBoardIcon size={64} strokeWidth="1" className="text-secondary" />
            <p className="text-center text-lg lg:text-xl">
              <FormattedMessage

                defaultMessage="Tell us more about your organization or team that would be the owner of this namespace."
                description="Description that is shown on the create namespace page"
              />
            </p>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <CreateNamespaceForm onCreate={onNamespaceCreate} />
        </CardContent>
      </Card>
    </LayoutContent>
  );
}
