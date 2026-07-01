import { z } from 'zod';
import { toast } from 'sonner';
import { FormattedMessage } from 'react-intl';
import { ShieldCheckIcon } from 'lucide-react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useClaimGroupVerification, useNamespace } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { GroupsBreadcrumbs } from '@konfigyr/components/groups/breadcrumbs';
import { GroupVerificationMethodSelector } from '@konfigyr/components/groups/group-verification-method';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { Button } from '@konfigyr/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import { Field, FieldDescription } from '@konfigyr/components/ui/field';
import { Input } from '@konfigyr/components/ui/input';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { CancelLabel } from '@konfigyr/components/messages';

import type { VerificationMethod } from '@konfigyr/hooks/types';

const claimSchema = z
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
    ({ groupId, verificationMethod }) => verificationMethod !== 'SOURCE_CODE' || /^io\.(github|gitlab|bitbucket)\.[a-z][a-z0-9_-]*$/.test(groupId),
    {
      message: 'Source code verification only supports io.github.*, io.gitlab.*, or io.bitbucket.* groupIds',
      path: ['groupId'],
    },
  );

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

  const form = useForm({
    defaultValues: { groupId: '', verificationMethod: 'DNS' as VerificationMethod },
    validators: { onSubmit: claimSchema },
    onSubmit: async ({ value }) => {
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
    },
  });

  const onSubmit = useFormSubmit(form);

  return (
    <LayoutContent>
      <LayoutNavbar title="Claim a groupId" />
      <div className="mx-4 space-y-6">
        <GroupsBreadcrumbs namespace={namespace}>
          <FormattedMessage
            defaultMessage="Claim a groupId"
            description="Breadcrumb label for the group verification claim creation page."
          />
        </GroupsBreadcrumbs>

        <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
          <Card className="border">
            <CardHeader>
              <CardTitle className="flex flex-col items-center gap-6 my-2">
                <ShieldCheckIcon size={64} strokeWidth="1" className="text-secondary" />
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
                <form name="claim-group-verification" className="grid gap-6" onSubmit={onSubmit}>
                  <form.AppField name="groupId" children={(field) => (
                    <field.Control
                      label={
                        <FormattedMessage
                          defaultMessage="groupId"
                          description="Label for the groupId input field on the group verification claim creation form."
                        />
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
                          autoFocus
                          autoComplete="off"
                          className="font-mono"
                          value={field.state.value}
                          onChange={e => field.handleChange(e.target.value)}
                          onBlur={() => field.handleBlur()}
                        />
                      }
                    />
                  )} />

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
                  )} />

                  <div className="flex gap-3">
                    <form.Submit>
                      <FormattedMessage
                        defaultMessage="Create claim"
                        description="Label for the submit button on the group verification claim creation form."
                      />
                    </form.Submit>
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => navigate({ to: '/namespace/$namespace/groups', params: { namespace: namespace.slug } })}
                    >
                      <CancelLabel />
                    </Button>
                  </div>
                </form>
              </form.AppForm>
            </CardContent>
          </Card>
        </div>
      </div>
    </LayoutContent>
  );
}
