import { useCallback, useId } from 'react';
import { SendIcon } from 'lucide-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useInviteNamespaceMember } from '@konfigyr/hooks';
import { NamespaceRole } from '@konfigyr/hooks/namespace/types';
import { useErrorNotification } from '@konfigyr/components/error';
import { NamespaceRoleLabel } from '@konfigyr/components/namespace/role';
import {
  Card,
  CardContent,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { useForm } from '@konfigyr/components/ui/form';

import type { FormEvent } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

interface Invitation {
  email: string;
  administrator: boolean;
}

export function InviteForm({ namespace }: { namespace: Namespace }) {
  const id = useId();
  const t = useIntl();
  const errorNotification = useErrorNotification();
  const { mutateAsync: inviteNamespaceMember } = useInviteNamespaceMember(namespace.slug);

  const form = useForm({
    defaultValues: { email: '', administrator: false } as Invitation,
    onSubmit: async ({ value }) => {
      try {
        await inviteNamespaceMember(value);
      } catch (error) {
        return errorNotification(error);
      }

      form.reset();
    },
  });

  const onSubmit = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();

    return form.handleSubmit(event);
  }, [form.handleSubmit]);

  return (
    <form.AppForm>
      <form name="invitiation-form" className="flex gap-6" onSubmit={onSubmit}>
        <form.AppField
          name="email"
          children={(field) => (
            <field.Control className="flex-1">
              <field.Input
                placeholder={t.formatMessage({
                  defaultMessage: 'Type in email address',
                  description: 'Placeholder for the email input field in the namespace members invite form',
                })}
                aria-label={t.formatMessage({
                  defaultMessage: 'Invite members by email address',
                  description: 'Label for the email input field in the namespace members invite form',
                })}
              />
            </field.Control>
          )}
        />

        <form.AppField
          name="administrator"
          children={(field) => (
            <field.Control className="flex min-w-34 items-start py-2">
              <div className="flex items-center space-x-2">
                <field.Switch aria-labelledby={`role-label-${id}`} />
                <field.Label id={`role-label-${id}`}>
                  <NamespaceRoleLabel role={field.state.value ? NamespaceRole.ADMIN : NamespaceRole.USER} />
                </field.Label>
              </div>
            </field.Control>
          )}
        />

        <form.Submit variant="ghost">
          <FormattedMessage
            defaultMessage="Invite"
            description="Submit button label for the namespace members invite form."
          />
        </form.Submit>
      </form>
    </form.AppForm>
  );
}

export function InviteFormCard({ namespace }: { namespace: Namespace }) {
  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CardIcon>
            <SendIcon size="1.25rem"/>
          </CardIcon>
          <FormattedMessage
            defaultMessage="Invite members"
            description="Label that is shown on the invite members form. Used as the card title."
          />
        </CardTitle>
      </CardHeader>
      <CardContent>
        <InviteForm namespace={namespace} />
      </CardContent>
    </Card>
  );
}
