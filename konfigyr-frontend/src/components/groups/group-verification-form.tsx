import { FormattedMessage } from 'react-intl';
import { ShieldCheckIcon } from 'lucide-react';
import { Button } from '@konfigyr/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import { Field, FieldDescription } from '@konfigyr/components/ui/field';
import { Input } from '@konfigyr/components/ui/input';
import { CancelLabel, SaveLabel } from '@konfigyr/components/messages';
import { GroupVerificationMethodSelector } from '@konfigyr/components/groups/group-verification-method';
import { z } from 'zod';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { GroupIdLabel } from '@konfigyr/components/groups/messages';

export const claimSchema = z
  .object({
    groupId: z
      .string()
      .min(1, { message: 'Group ID is required' })
      .regex(/^[a-z][a-z0-9_\-.]*(\.[a-z][a-z0-9_\-.]*)+$/, {
        message: 'Must be lowercase, dot-separated, with at least two segments — e.g. com.mycompany',
      }),
    verificationMethod: z.enum(['DNS', 'SOURCE_CODE']),
  })
  .refine(
    ({
      groupId,
      verificationMethod,
    }) => verificationMethod !== 'SOURCE_CODE' || /^io\.(github|gitlab|bitbucket)\.[a-z][a-z0-9_-]*$/.test(groupId),
    {
      message: 'Source code verification only supports io.github.*, io.gitlab.*, or io.bitbucket.* groupIds',
      path: ['groupId'],
    },
  );

export type GroupVerificationFormValues = z.infer<typeof claimSchema>;

export type GroupVerificationFormProps = {
  defaultValues: GroupVerificationFormValues;
  onSubmit: (args: { value: GroupVerificationFormValues }) => Promise<void>;
  onCancel: () => void;
};

export function GroupVerificationForm ({
  defaultValues,
  onSubmit,
  onCancel,
}: GroupVerificationFormProps) {
  const form = useForm({
    defaultValues,
    validators: { onSubmit: claimSchema },
    onSubmit,
  });

  const handleSubmit = useFormSubmit(form);
  const isGroupIdEditable = !!defaultValues.groupId;

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex flex-col items-center gap-6 my-2">
          <ShieldCheckIcon size={64} strokeWidth="1" className="text-secondary"/>
          <p className="text-center text-lg lg:text-xl">
            <FormattedMessage
              defaultMessage="Claim a Maven groupId"
              description="Heading on the group verification claim creation form"
            />
          </p>
        </CardTitle>
        <CardDescription>
          <FormattedMessage
            defaultMessage="Enter the Maven coordinate you want to publish under. After submitting, you'll verify ownership using your chosen verification method."
            description="Description text shown when user tries to create a new group verification claim"
          />
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form.AppForm>
          <form name="claim-group-verification" className="grid gap-6" onSubmit={handleSubmit}>
            <form.AppField name="groupId" children={(field) => (
              <field.Control
                label={
                  <GroupIdLabel/>
                }
                description={
                  <FormattedMessage
                    defaultMessage="Lowercase, dot-separated, at least two segments — e.g. com.mycompany"
                    description="Hint text for the groupId input field on the group verification claim creation form."
                  />
                }
                render={
                  <Input
                    placeholder="com.mycompany"
                    autoFocus={isGroupIdEditable}
                    autoComplete="off"
                    className="font-mono"
                    readOnly={isGroupIdEditable}
                    disabled={isGroupIdEditable}
                    value={field.state.value}
                    onChange={e => field.handleChange(e.target.value)}
                    onBlur={() => field.handleBlur()}
                  />
                }
              />
            )}/>

            <form.AppField name="verificationMethod" children={(field) => (
              <Field>
                <FieldDescription>
                  <FormattedMessage
                    defaultMessage="Verification method"
                    description="Label for the verification method selector on the group verification claim creation form."
                  />
                </FieldDescription>
                <GroupVerificationMethodSelector
                  value={field.state.value}
                  onChange={field.handleChange}
                />
              </Field>
            )}/>

            <div className="flex gap-3">
              <form.Submit>
                <SaveLabel/>
              </form.Submit>
              <Button type="button" variant="ghost" onClick={onCancel}>
                <CancelLabel/>
              </Button>
            </div>
          </form>
        </form.AppForm>
      </CardContent>
    </Card>
  );
}
