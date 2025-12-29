import { useCallback } from 'react';
import { FolderKeyIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { useNamespace } from '@konfigyr/hooks';
import { CreateKeysetLabel } from '@konfigyr/components/kms/messages';
import { CreateKeysetForm } from '@konfigyr/components/kms/keyset-create';
import { createFileRoute, useNavigate } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/kms/create',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const navigate = useNavigate();

  const onKeysetCreate = useCallback(async () => {
    await navigate({
      to: '/namespace/$namespace/kms',
      params: { namespace: namespace.slug },
    });
  }, [namespace.slug, navigate]);

  return (
    <div className="w-full lg:w-3/5 xl:w-1/2 space-y-6 px-4 mx-auto">
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex flex-col items-center gap-6 my-2">
            <FolderKeyIcon size={64} strokeWidth="1" className="text-secondary" />
            <p className="text-center text-lg lg:text-xl">
              <CreateKeysetLabel />
            </p>
          </CardTitle>
          <CardDescription>
            <FormattedMessage
              defaultMessage="Create a managed container for your encryption keys. Instead of managing raw keys, you manage a keyset that supports automatic rotation and versioning."
              description="Modal description text that is shown when user tries to create a new keyset"
            />
          </CardDescription>
        </CardHeader>
        <CardContent>
          <CreateKeysetForm namespace={namespace} onCreate={onKeysetCreate} />
        </CardContent>
      </Card>
    </div>
  );
}
