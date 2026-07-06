import { FormattedMessage } from 'react-intl';
import { toast } from 'sonner';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useClaimGroupVerification, useNamespace } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { GroupsBreadcrumbs } from '@konfigyr/components/groups/breadcrumbs';
import { GroupVerificationForm } from '@konfigyr/components/groups/group-verification-form';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { GroupClaimsLabel } from '@konfigyr/components/groups/messages';
import type { GroupVerificationFormValues } from '@konfigyr/components/groups/group-verification-form';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/groups/create',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const navigate = useNavigate({ from: Route.fullPath });
  const errorNotification = useErrorNotification();
  const { mutateAsync: claimGroupVerification } = useClaimGroupVerification(namespace.slug);

  const defaultValues: GroupVerificationFormValues = { groupId: '', verificationMethod: 'DNS' };

  const onSubmit = async ({ value }: { value: typeof defaultValues }) => {
    try {
      const created = await claimGroupVerification(value);

      toast.success((
        <FormattedMessage
          defaultMessage="Claim for {groupId} was created"
          description="Notification message that is shown when a group verification claim was successfully created"
          values={{ groupId: created.groupId }}
        />
      ));

      await navigate({
        to: '/namespace/$namespace/groups/$groupId',
        params: { namespace: namespace.slug, groupId: created.groupId },
      });
    } catch (error) {
      errorNotification(error);
    }
  };

  return (
    <LayoutContent>
      <LayoutNavbar title={( <GroupClaimsLabel /> )} />
      <div className="mx-4 space-y-6">
        <GroupsBreadcrumbs namespace={namespace}>
          <FormattedMessage
            defaultMessage="Claim a group id"
            description="Breadcrumb label for the group verification claim creation page."
          />
        </GroupsBreadcrumbs>

        <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
          <GroupVerificationForm
            defaultValues={defaultValues}
            onSubmit={onSubmit}
            onCancel={() => navigate({ to: '/namespace/$namespace/groups', params: { namespace: namespace.slug } })}
          />
        </div>
      </div>
    </LayoutContent>
  );
}
