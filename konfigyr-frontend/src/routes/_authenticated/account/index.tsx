import { useAccount } from '@konfigyr/hooks';
import { AccountDeleteConfirmationDialog } from '@konfigyr/components/account/delete-dialog';
import { AccountEmailForm } from '@konfigyr/components/account/email-form';
import { Goodbye } from '@konfigyr/components/account/goodbye';
import { AccountNameForm } from '@konfigyr/components/account/name-form';
import { LayoutContent } from '@konfigyr/components/layout';
import { useMutation } from '@tanstack/react-query';
import { createServerFn, useServerFn } from '@tanstack/react-start';
import { createFileRoute } from '@tanstack/react-router';
import { onDeleteAccount } from './-handler';

const deleteAccount = createServerFn({ method: 'POST' })
  .handler(() => onDeleteAccount());

export const Route = createFileRoute('/_authenticated/account/')({
  component: RouteComponent,
  head: () => ({
    meta: [{
      title: 'Account settings | Konfigyr',
    }],
  }),
});

function RouteComponent() {
  const account = useAccount();
  const action = useServerFn(deleteAccount);

  const { mutateAsync, isSuccess } = useMutation<void, void>({
    mutationFn: async () => {
      const response = await action();

      if (response.status !== 204) {
        throw await response.json();
      }
    },
  });

  if (isSuccess) {
    return (
      <Goodbye />
    );
  }

  return (
    <LayoutContent variant="centered" className="space-y-8">
      <AccountEmailForm account={account} />
      <AccountNameForm account={account} />
      <AccountDeleteConfirmationDialog
        account={account}
        onDelete={mutateAsync}
      />
    </LayoutContent>
  );
}
