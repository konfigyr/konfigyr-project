import { FormattedMessage } from 'react-intl';
import { ArrowLeftRightIcon } from 'lucide-react';
import { createFileRoute } from '@tanstack/react-router';
import { useGetTransfer, useNamespace } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { TransfersBreadcrumbs } from '@konfigyr/components/artifactory/transfers/breadcrumbs';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { TransferDetails } from '@konfigyr/components/artifactory/transfers/transfer-details';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/transfers/$transferId/',
)({
  component: RouteComponent,
});

function RouteComponent () {
  const namespace = useNamespace();
  const { transferId } = Route.useParams();

  const { data: transfer, error, isError, isPending } = useGetTransfer(namespace.slug, transferId);

  return (
    <>
      <TransfersBreadcrumbs namespace={namespace}>
        {transferId}
      </TransfersBreadcrumbs>

      <div className="mx-auto w-full space-y-6 lg:w-2/3 xl:w-3/5">
        {isPending && (
          <EmptyState
            icon={<ArrowLeftRightIcon/>}
            title={(
              <FormattedMessage
                defaultMessage="Loading ownership transfer"
                description="Loading state title for an ownership transfer detail page."
              />
            )}
            description={(
              <FormattedMessage
                defaultMessage="Fetching the current transfer request."
                description="Loading state description for an ownership transfer detail page."
              />
            )}
          />
        )}

        {isError && (
          <ErrorState error={error} className="border-none"/>
        )}

        {transfer && (
          <TransferDetails transfer={transfer} namespace={namespace} />
        )}
      </div>
    </>
  );
}
