import { toast } from 'sonner';
import { useCallback, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { useUpdateProfile } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@konfigyr/components/ui/dialog';

import { FieldGroup, FieldSet } from '@konfigyr/components/ui/field';
import { PolicyPicker } from '@konfigyr/components/vault/profile/policy-picker';
import type { Service } from '@konfigyr/hooks/namespace/types';
import type { Namespace, Profile, ProfilePolicy } from '@konfigyr/hooks/types';

type Props = {
  namespace: Namespace,
  service: Service,
  profile?: Profile,
  onClose: () => void
};

const AVAILABLE_POLICIES: Array<ProfilePolicy> = ['PROTECTED', 'UNPROTECTED'] as const;

export function UpdateConfigurationProfilePolicyDialog({ namespace, service, profile, onClose }: Props) {
  const open = useMemo(() => !!profile, [profile]);
  const errorNotification = useErrorNotification();
  const { mutateAsync: updateProfile } = useUpdateProfile(namespace, service, profile!);

  const form = useForm({
    defaultValues: {
      policy: profile?.policy || 'UNPROTECTED',
    } as Partial<Profile>,

    onSubmit: async ({ value }: { value: Partial<Profile> }) => {
      try {
        await updateProfile({
          id: profile!.id,
          policy: value.policy,
        });

      } catch (error) {
        return errorNotification(error);
      }

      toast.success(<FormattedMessage
        defaultMessage="Successfully updated profile policy"
        description="Success message when profile policy is updated"
      />);

      return onOpenChange(false);
    },
  });

  const onSubmit = useFormSubmit(form);

  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
      form.reset();
    }
  }, [form, profile, onClose]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form.AppForm>
          <form onSubmit={onSubmit}>
            <DialogHeader>
              <DialogTitle>
                <FormattedMessage
                  defaultMessage="Change the policy for the {name} profile?"
                  values={{ name: profile?.name }}
                  description="Title of the modal that is shown when user tries to change a policy of a profile"
                />
              </DialogTitle>
            </DialogHeader>
            <div className="py-4">
              <form.AppField name="policy" children={() => (
                <FieldSet>
                  <FieldGroup>
                    <form.AppField name="policy" children={(field) => (
                      <PolicyPicker
                        options={AVAILABLE_POLICIES}
                        value={field.state.value}
                        onChange={field.setValue}
                      />
                    )} />
                  </FieldGroup>
                </FieldSet>
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
                      defaultMessage="Update policy"
                      description="Label for the submit button in the modal that is shown when user tries to change a policy of a profile"
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
