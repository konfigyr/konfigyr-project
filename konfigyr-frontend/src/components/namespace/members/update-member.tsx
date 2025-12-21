import { toast } from 'sonner';
import { useCallback, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { useUpdateNamespaceMember } from '@konfigyr/hooks';
import { NamespaceRole} from '@konfigyr/hooks/namespace/types';
import { useErrorNotification } from '@konfigyr/components/error';
import { NamespaceRoleDescription, NamespaceRoleLabel } from '@konfigyr/components/namespace/role';
import { Button } from '@konfigyr/components/ui/button';
import { Label } from '@konfigyr/components/ui/label';
import { useForm } from '@konfigyr/components/ui/form';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@konfigyr/components/ui/dialog';
import { RadioGroup, RadioGroupItem } from '@konfigyr/components/ui/radio-group';

import type { Member, Namespace } from '@konfigyr/hooks/types';

export function UpdateMemberForm({ namespace, member, onClose }: { namespace: Namespace, member?: Member | null, onClose: () => void }) {
  const open = useMemo(() => !!member, [member]);
  const roles = Object.values(NamespaceRole);
  const errorNotification = useErrorNotification();
  const { mutateAsync: updateNamespaceMember } = useUpdateNamespaceMember(namespace.slug);

  const form = useForm({
    defaultValues: {
      role: member?.role || NamespaceRole.USER,
    },
    onSubmit: async ({ value }) => {
      try {
        await updateNamespaceMember({ id: member!.id, role: value.role });
      } catch (error) {
        console.error(error);
        return errorNotification(error);
      }

      toast.success(<FormattedMessage
        defaultMessage="Successfully updated member role"
        values={{ name: member?.fullName, role: value.role, namespace: namespace.name }}
        description="Success message when member role is updated"
      />);

      return onOpenChange(false);
    },
  });

  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
      form.reset();
    }
  }, [form, member, onClose]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form.AppForm>
          <form onSubmit={event => {
            event.preventDefault();
            event.stopPropagation();
            return form.handleSubmit(event);
          }}>
            <DialogHeader>
              <DialogTitle>
                <FormattedMessage
                  defaultMessage="Change the role of {name}?"
                  values={{ name: member?.fullName }}
                  description="Title of the modal that is shown when user tries to change the role of a namespace member"
                />
              </DialogTitle>
              <DialogDescription>
                <FormattedMessage
                  defaultMessage="Select a new role for this member."
                  description="Modal description text that is shown when user tries to change the role of a namespace member"
                />
              </DialogDescription>
            </DialogHeader>
            <div className="py-4">
              <form.AppField name="role" children={(field) => (
                <RadioGroup
                  defaultValue={field.state.value}
                  onValueChange={value => field.handleChange(value as NamespaceRole)}
                >
                  {roles.map(role => (
                    <div key={role} className="flex items-start space-x-2">
                      <RadioGroupItem value={role} id={role}/>
                      <div className="space-y-1">
                        <Label htmlFor={role}>
                          <NamespaceRoleLabel role={role}/>
                        </Label>
                        <p className="text-muted-foreground text-sm">
                          <NamespaceRoleDescription role={role}/>
                        </p>
                      </div>
                    </div>
                  ))}
                </RadioGroup>
              )}/>
            </div>
            <DialogFooter>
              <form.Subscribe
                selector={state => [state.isValid, state.isValidating, state.isSubmitting]}
                children={([isValid, isValidating, isSubmitting]) => (
                  <Button
                    type="submit"
                    disabled={!isValid}
                    loading={isSubmitting || isValidating}
                  >
                    <FormattedMessage
                      defaultMessage="Update role"
                      description="Label for the submit button in the modal that is shown when user tries to change the role of a namespace member"
                    />
                  </Button>
                )}
              />
            </DialogFooter>
          </form>
        </form.AppForm>
      </DialogContent>
    </Dialog>
  );
}
