import { FormattedMessage } from 'react-intl';
import { toast } from 'sonner';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import {
  useClaimGroupVerification,
  useGetGroupVerification,
  useGetVerificationChallenges,
  useNamespace,
} from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { GroupsBreadcrumbs } from '@konfigyr/components/groups/breadcrumbs';
import { GroupVerificationForm } from '@konfigyr/components/groups/group-verification-form';
import { ClaimAgainLabel, EditClaimLabel } from '@konfigyr/components/groups/messages';
import { BreadcrumbItem, BreadcrumbLink, BreadcrumbSeparator } from '@konfigyr/components/ui/breadcrumb';
import { GroupVerificationDetailsLink } from '@konfigyr/components/groups/group-verification-table';
import { useMemo } from 'react';
import type { GroupVerificationFormValues } from '@konfigyr/components/groups/group-verification-form';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/groups/$groupId/edit',
)({
  component: RouteComponent,
});

function RouteComponent () {
  const namespace = useNamespace();
  const { groupId } = Route.useParams();
  const navigate = useNavigate({ from: Route.fullPath });
  const errorNotification = useErrorNotification();
  const { mutateAsync: claimGroupVerification } = useClaimGroupVerification(namespace.slug);

  const {
    data: verification,
  } = useGetGroupVerification(namespace.slug, groupId);

  const {
    data: challenges,
  } = useGetVerificationChallenges(namespace.slug, groupId);

  const challenge = useMemo(() => {
    const items = challenges?.data ?? [];
    // items already ordered on the backend by createdAt.asc(),
    return items.at(-1);
  }, [challenges]);

  const defaultValues: GroupVerificationFormValues = {
    groupId: verification?.groupId ?? '',
    verificationMethod: challenge?.method ?? 'DNS',
  };

  const onSubmit = async ({ value }: { value: typeof defaultValues }) => {
    try {
      const updated = await claimGroupVerification(value);

      toast.success((
        <FormattedMessage
          defaultMessage="Claim for {groupId} was updated"
          description="Notification message that is shown when a group verification claim was successfully updated from the edit page."
          values={{ groupId: updated.groupId }}
        />
      ));

      await navigate({
        to: '/namespace/$namespace/groups/$groupId',
        params: { namespace: namespace.slug, groupId: updated.groupId },
      });
    } catch (error) {
      errorNotification(error);
    }
  };

  return (
    <>
      <GroupsBreadcrumbs namespace={namespace}>
        <BreadcrumbItem>
          <BreadcrumbLink
            render={
              <GroupVerificationDetailsLink namespace={namespace.slug} groupId={groupId} />
            }
          />
        </BreadcrumbItem>
        <BreadcrumbSeparator />
        <ClaimAgainLabel />
      </GroupsBreadcrumbs>

      <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
        <GroupVerificationForm
          defaultValues={defaultValues}
          onSubmit={onSubmit}
          onCancel={() => navigate({ to: '/namespace/$namespace/groups/$groupId', params: { namespace: namespace.slug, groupId } })}
        />
      </div>
    </>
  );
}
