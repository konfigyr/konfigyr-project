import { z } from 'zod';
import { useIntl } from 'react-intl';
import { CheckIcon } from 'lucide-react';
import { CancelLabel } from '@konfigyr/components/messages';
import { PolicyTooltip } from '@konfigyr/components/vault/profile/policy-tooltip';
import { Button } from '@konfigyr/components/ui/button';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/components/ui/dialog';
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from '@konfigyr/components/ui/field';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { RadioGroup, RadioGroupItem } from '@konfigyr/components/ui/radio-group';
import { ScrollArea } from '@konfigyr/components/ui/scroll-area';

import type { ChangesetState, Profile } from '@konfigyr/hooks/types';

enum Operation {
  APPLY = 'APPLY',
  SUBMIT = 'SUBMIT',
}

const changesetSchema = z.object({
  name: z.string()
    .min(2, { message: 'Name must be at least 2 characters.' })
    .max(30, { message: 'Name must be at most 30 characters.' }),
  description: z.string()
    .max(255, { message: 'Description must be at most 255 characters.' }),
  operation: z.enum([
    Operation.APPLY,
    Operation.SUBMIT,
  ]),
});

const resolveDefaultOperation = (profile: Profile) => {
  if (profile.policy === 'UNPROTECTED') {
    return Operation.APPLY;
  }
  if (profile.policy === 'PROTECTED') {
    return Operation.SUBMIT;
  }
  return '';
};

interface ChangesetSubmitDialogProps {
  changeset: ChangesetState;
  onApply: (changeset: ChangesetState) => void | Promise<any>;
  onSubmit: (changeset: ChangesetState) => void | Promise<any>;
  onError: (error: Error) => void;
}

function ChangesetSubmitDialogForm({ changeset, onApply, onSubmit, onError }: ChangesetSubmitDialogProps) {
  const isProtectedProfile = changeset.profile.policy !== 'UNPROTECTED';
  const isImmutableProfile = changeset.profile.policy === 'IMMUTABLE';

  const intl = useIntl();
  const form = useForm({
    defaultValues: {
      name: changeset.name,
      description: '',
      operation: resolveDefaultOperation(changeset.profile),
    },
    validators: {
      onSubmit: changesetSchema,
      onMount: changesetSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        if (value.operation === Operation.APPLY) {
          await onApply({ ...changeset, name: value.name, description: value.description });
        }
        if (value.operation === Operation.SUBMIT) {
          await onSubmit({ ...changeset, name: value.name, description: value.description });
        }
      } catch (error) {
        onError(error as Error);
      }
    },
  });

  const onFormSubmit = useFormSubmit(form);

  return (
    <form.AppForm>
      <form className="grid gap-4 w-full" name="submit-changeset-dialog-form" onSubmit={onFormSubmit}>
        <DialogHeader>
          <DialogTitle>Submit configuration changes</DialogTitle>
        </DialogHeader>
        <ScrollArea className="-mx-4 max-h-[80vh] overflow-y-auto">
          <FieldGroup className="px-4">
            <form.AppField
              name="name"
              children={(field) => (
                <field.Control
                  label={intl.formatMessage({
                    defaultMessage: 'Change subject',
                    description: 'Label for the submit changeset subject form field',
                  })}
                  description={intl.formatMessage({
                    defaultMessage: 'A short summary of what this change does. This will appear in the change history.',
                    description: 'Help text for the submit changeset subject form field',
                  })}
                  render={
                    <field.Input
                      placeholder={intl.formatMessage({
                        defaultMessage: 'e.g. Update database connection settings',
                        description: 'Placeholder for the submit changeset subject form field',
                      })}
                    />
                  }
                />
              )}
            />

            <form.AppField
              name="description"
              children={(field) => (
                <field.Control
                  label={intl.formatMessage({
                    defaultMessage: 'Description',
                    description: 'Label for the submit changeset description form field',
                  })}
                  description={intl.formatMessage({
                    defaultMessage: 'Provide additional details about the change. This helps others understand the intent.',
                    description: 'Help text for the submit changeset description form field',
                  })}
                  render={
                    <field.Textarea
                      rows={4}
                      placeholder={intl.formatMessage({
                        defaultMessage: 'Describe the reason for this change and any relevant context…',
                        description: 'Placeholder for the submit changeset description form field',
                      })}
                    />
                  }
                />
              )}
            />

            <form.AppField
              name="operation"
              children={(field) => (
                <RadioGroup
                  defaultValue={field.state.value}
                  onValueChange={value => field.handleChange(value as Operation)}
                >
                  <Field orientation="horizontal" data-disabled={isProtectedProfile}>
                    <RadioGroupItem
                      id="operation-apply"
                      value={Operation.APPLY}
                      disabled={isProtectedProfile}
                    />
                    <FieldContent>
                      <PolicyTooltip
                        profile={changeset.profile}
                        disabled={!isProtectedProfile}
                        render={
                          <FieldLabel htmlFor="operation-apply">
                            {intl.formatMessage({
                              defaultMessage: 'Apply changes directly',
                              description: 'Label for the submit changeset apply operation form radio option',
                            })}
                          </FieldLabel>
                        }
                      />
                      <FieldDescription>
                        {intl.formatMessage({
                          defaultMessage: 'The changes will be applied immediately to the profile without review. Use this for low-risk or urgent updates.',
                          description: 'Help text for the submit changeset apply operation form radio option',
                        })}
                      </FieldDescription>
                    </FieldContent>
                  </Field>

                  <Field orientation="horizontal" data-disabled={isImmutableProfile}>
                    <RadioGroupItem
                      id="operation-submit"
                      value={Operation.SUBMIT}
                      disabled={isImmutableProfile}
                    />
                    <FieldContent>
                      <FieldLabel htmlFor="operation-submit">
                        {intl.formatMessage({
                          defaultMessage: 'Submit for review',
                          description: 'Label for the submit changeset submit operation form radio option',
                        })}
                      </FieldLabel>
                      <FieldDescription>
                        {intl.formatMessage({
                          defaultMessage: 'A change request will be created. The changes must be reviewed and approved before they are applied.',
                          description: 'Help text for the submit changeset create changeset operation form radio option',
                        })}
                      </FieldDescription>
                    </FieldContent>
                  </Field>
                </RadioGroup>
              )}
            />
          </FieldGroup>
        </ScrollArea>
        <DialogFooter>
          <DialogClose
            render={
              <Button variant="outline">
                <CancelLabel />
              </Button>
            }
          />
          <form.Submit>
            {intl.formatMessage({
              defaultMessage: 'Propose changes',
              description: 'Label for the submit changeset submit form button',
            })}
          </form.Submit>
        </DialogFooter>
      </form>
    </form.AppForm>
  );
}

export function ChangesetSubmitDialog({
  changeset,
  disabled,
  open,
  onOpenChange,
  ...props
}: ChangesetSubmitDialogProps & {
  disabled?: boolean;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}) {
  const intl = useIntl();

  if (changeset.profile.policy === 'IMMUTABLE') {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger
        render={
          <Button size="sm" disabled={disabled}>
            <CheckIcon />
            {changeset.profile.policy === 'UNPROTECTED' ? (
              intl.formatMessage({
                defaultMessage: 'Apply',
                description: 'Label for the apply button in the changeset status bar.',
              })
            ) : (
              intl.formatMessage({
                defaultMessage: 'Submit',
                description: 'Label for the submit button in the changeset status bar.',
              })
            )}
          </Button>
        }
      />

      <DialogContent className="sm:min-w-lg">
        <ChangesetSubmitDialogForm
          changeset={changeset}
          {...props}
        />
      </DialogContent>
    </Dialog>
  );
}
