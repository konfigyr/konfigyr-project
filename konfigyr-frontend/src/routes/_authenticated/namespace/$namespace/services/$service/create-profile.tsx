import { toast } from 'sonner';
import { useCallback  } from 'react';
import { VaultIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { CreateProfileForm } from '@konfigyr/components/vault/profile/create-form';
import { createFileRoute, useNavigate } from '@tanstack/react-router';

import type { Profile } from '@konfigyr/hooks/types';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/create-profile',
)({
  head: () => ({
    meta: [{
      title: 'Create profile | Konfigyr',
    }],
  }),
  preload: false,
  component: RouteComponent,
});

function RouteComponent() {
  const navigate = useNavigate();
  const { namespace, service } = Route.parentRoute.useLoaderData();

  const onProfileCreate = useCallback(async (profile: Profile) => {
    toast.success(<FormattedMessage
      defaultMessage="{name} was successfully created"
      values={{ name: profile.name }}
      description="Notification message shown when profile was successfully created"
    />);

    await navigate({
      to: '/namespace/$namespace/services/$service/profiles/$profile',
      params: { namespace: namespace.slug, service: service.slug, profile: profile.slug },
    });
  }, [navigate, namespace.slug, service.slug]);

  return (
    <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex flex-col items-center gap-6 my-2">
            <VaultIcon size={64} strokeWidth="1" className="text-secondary" />
            <p className="text-center text-lg lg:text-xl">
              <FormattedMessage
                defaultMessage="New configuration profile"
                description="Title text that is shown when user tries to create a new profile"
              />
            </p>
          </CardTitle>
          <CardDescription>
            <FormattedMessage
              defaultMessage="A profile defines a separate configuration state for this service. They are used to safely manage environment-specific values and control how changes are applied."
              description="Description text that is shown when user tries to create a new profile"
            />
          </CardDescription>
        </CardHeader>
        <CardContent>
          <CreateProfileForm
            namespace={namespace}
            service={service}
            onCreate={onProfileCreate}
          />
        </CardContent>
      </Card>
    </div>
  );
}
