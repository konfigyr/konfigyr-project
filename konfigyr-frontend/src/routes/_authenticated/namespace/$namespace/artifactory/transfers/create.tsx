import { z } from 'zod';
import { useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { toast } from 'sonner';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useNamespace, useRequestTransfer } from '@konfigyr/hooks';
import { TransfersBreadcrumbs } from '@konfigyr/components/artifactory/transfers/breadcrumbs';
import { TransferRequestForm } from '@konfigyr/components/artifactory/transfers/transfer-request-form';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { TransferRequestedSuccessMessage, TransfersLabel } from '@konfigyr/components/artifactory/transfers/messages';
import type { TransferRequestFormValues } from '@konfigyr/components/artifactory/transfers/transfer-request-form';

const searchQuerySchema = z.object({
  groupId: z.string().optional().catch(undefined),
  fromNamespace: z.string().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/transfers/create',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const navigate = useNavigate({ from: Route.fullPath });
  const search = Route.useSearch();
  const [error, setError] = useState<unknown>();
  const { mutateAsync: requestTransfer } = useRequestTransfer(namespace.slug);

  const defaultValues: TransferRequestFormValues = {
    groupId: search.groupId ?? '',
    fromNamespace: search.fromNamespace ?? '',
  };

  const onCancel = async () => navigate({
    to: '/namespace/$namespace/artifactory/transfers',
    params: { namespace: namespace.slug },
  });

  const onSubmit = async ({ value }: { value: typeof defaultValues }) => {
    setError(undefined);

    let created;
    try {
      created = await requestTransfer(value);
    } catch (err) {
      setError(err);
      return;
    }

    toast.success((
      <TransferRequestedSuccessMessage groupId={created.groupId}/>
    ));

    await navigate({
      to: '/namespace/$namespace/artifactory/transfers/$transferId',
      params: { namespace: namespace.slug, transferId: created.id },
    });
  };

  return (
    <LayoutContent>
      <LayoutNavbar title={( <TransfersLabel/> )}/>
      <div className="mx-4 space-y-6">
        <TransfersBreadcrumbs namespace={namespace}>
          <FormattedMessage
            defaultMessage="Request a transfer"
            description="Breadcrumb label for the ownership transfer request creation page."
          />
        </TransfersBreadcrumbs>

        <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
          <TransferRequestForm
            namespace={namespace.slug}
            defaultValues={defaultValues}
            onSubmit={onSubmit}
            onCancel={onCancel}
            error={error}
          />
        </div>
      </div>
    </LayoutContent>
  );
}
