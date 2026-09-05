import { z } from 'zod';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { ArrowLeftRightIcon, CircleQuestionMarkIcon } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useDebouncedCallback } from 'use-debounce';
import { Link } from '@tanstack/react-router';
import { Button, buttonVariants } from '@konfigyr/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import { SimpleAlert } from '@konfigyr/components/ui/alert';
import { useNormalizeError } from '@konfigyr/components/error/normalize';
import { CancelLabel } from '@konfigyr/components/messages';
import { Field, FieldDescription, FieldError, FieldLabel } from '@konfigyr/components/ui/field';
import { Combobox, ComboboxContent, ComboboxEmpty, ComboboxInput, ComboboxItem, ComboboxList } from '@konfigyr/components/ui/combobox';
import { Label } from '@konfigyr/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@konfigyr/components/ui/radio-group';
import { cn } from '@konfigyr/components/utils';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { getGroupVerification, useGetGroupVerifications } from '@konfigyr/hooks';
import { GroupIdLabel, RequestTransferLabel } from '@konfigyr/components/artifactory/transfers/messages';

export const TRANSFER_REQUEST_SCHEMA = z.object({
  groupId: z.string().min(1, { message: 'Group ID is required' }),
  fromNamespace: z.string().min(1, { message: 'Namespace is required' }),
});

export type TransferRequestFormValues = z.infer<typeof TRANSFER_REQUEST_SCHEMA>;

export type TransferRequestFormProps = {
  namespace: string;
  defaultValues: TransferRequestFormValues;
  onSubmit: (args: { value: TransferRequestFormValues }) => Promise<void>;
  onCancel: () => void;
  error?: unknown;
};

function normalizeFieldError(e: unknown): { message?: string } {
  if (typeof e === 'string') return { message: e };
  if (e && typeof e === 'object' && 'message' in e) return e as { message?: string };
  return {};
}

function GroupIdCombobox({ namespace, value, onChange }: {
  namespace: string;
  value: string;
  onChange: (value: string) => void;
}) {
  const [input, setInput] = useState(value);
  const [term, setTerm] = useState<string>();
  const debouncedSetTerm = useDebouncedCallback((next: string) => setTerm(next || undefined), 200);

  useEffect(() => setInput(value), [value]);

  const { data } = useGetGroupVerifications(namespace, { state: 'ACTIVE', term, size: 20 });

  const groupIds = useMemo(() => {
    const ids = new Set((data?.data ?? []).map(verification => verification.groupId));
    if (value) {
      ids.add(value);
    }
    return Array.from(ids);
  }, [data, value]);

  const onInputValueChange = useCallback((next: string) => {
    setInput(next);
    debouncedSetTerm(next);
  }, [debouncedSetTerm]);

  return (
    <Combobox
      items={groupIds}
      value={value || null}
      onValueChange={(next) => onChange(next ?? '')}
      inputValue={input}
      onInputValueChange={onInputValueChange}
    >
      <ComboboxInput
        aria-label="Group Id"
        placeholder="com.mycompany"
        autoComplete="off"
        className="font-mono"
      />
      <ComboboxContent>
        <ComboboxEmpty className="flex-col gap-2 py-3">
          <FormattedMessage
            defaultMessage="No active group claims found."
            description="Empty state shown in the groupId combobox when this namespace has no matching active claims."
          />
          <Link
            to="/namespace/$namespace/artifactory/groups/create"
            params={{ namespace }}
            className={buttonVariants({ variant: 'outline', size: 'sm' })}
          >
            <FormattedMessage
              defaultMessage="Claim a groupId"
              description="Link shown in the groupId combobox empty state, leading to the group verification claim creation page."
            />
          </Link>
        </ComboboxEmpty>
        <ComboboxList>
          {groupIds.map(groupId => (
            <ComboboxItem key={groupId} value={groupId}>{groupId}</ComboboxItem>
          ))}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  );
}

function FromNamespaceRadioGroup({ namespace, groupId, value, onChange }: {
  namespace: string;
  groupId: string;
  value: string;
  onChange: (value: string) => void;
}) {
  const { data: verification, isPending } = useQuery({
    ...getGroupVerification(namespace, groupId),
    enabled: !!groupId,
  });

  const owners = useMemo(() => {
    const slugs = new Set(verification?.conflictingOwners ?? []);
    if (value) {
      slugs.add(value);
    }
    return Array.from(slugs);
  }, [verification, value]);

  if (!groupId) {
    return (
      <p className="text-sm text-muted-foreground italic">
        <FormattedMessage
          defaultMessage="Select a groupId first."
          description="Hint shown before a groupId has been chosen on the ownership transfer request creation form."
        />
      </p>
    );
  }

  if (isPending) {
    return (
      <p className="text-sm text-muted-foreground italic">
        <FormattedMessage
          defaultMessage="Checking for owners of this groupId, please be patient..."
          description="Loading state shown when the selected groupId is loading conflicting groupId owners."
        />
      </p>
    );
  }

  if (owners.length === 0) {
    return (
      <p className="text-sm text-muted-foreground italic">
        <FormattedMessage
          defaultMessage="No known namespaces own artifacts under this groupId yet."
          description="Empty state shown when the selected groupId has no conflicting owners."
        />
      </p>
    );
  }

  return (
    <RadioGroup value={value || ''} onValueChange={onChange} aria-label="From namespace">
      {owners.map((slug) => {
        const selected = value === slug;
        return (
          <Label
            key={slug}
            className={cn(
              'cursor-pointer border gap-4 rounded-xl p-4 transition-colors',
              selected ? 'border-primary bg-primary/5 dark:bg-primary/10' : 'border-transparent hover:bg-muted/50',
            )}
          >
            <RadioGroupItem value={slug}/>
            <span className="font-medium font-mono leading-none">{slug}</span>
          </Label>
        );
      })}
    </RadioGroup>
  );
}

export function TransferRequestForm ({
  namespace,
  defaultValues,
  onSubmit,
  onCancel,
  error,
}: TransferRequestFormProps) {
  const form = useForm({
    defaultValues,
    validators: { onSubmit: TRANSFER_REQUEST_SCHEMA },
    onSubmit,
  });

  const handleSubmit = useFormSubmit(form);
  const problem = useNormalizeError(error);

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex flex-col items-center gap-6 my-2">
          <ArrowLeftRightIcon size={64} strokeWidth="1" className="text-secondary"/>
          <p className="text-center text-lg lg:text-xl">
            <FormattedMessage
              defaultMessage="Request an ownership transfer"
              description="Heading on the ownership transfer request creation form"
            />
          </p>
        </CardTitle>
        <CardDescription>
          <FormattedMessage
            defaultMessage="Ask another namespace to release a Maven groupId so this namespace can take over publishing under it."
            description="Description text shown when user tries to create a new ownership transfer request"
          />
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <SimpleAlert
          icon={<CircleQuestionMarkIcon />}
          title={
            <FormattedMessage
              defaultMessage="How ownership transfer works"
              description="Title for the explanation of the ownership transfer preconditions shown above the request form."
            />
          }
          description={
            <FormattedMessage
              defaultMessage="You can only request a transfer for a groupId your own namespace already holds an active, verified claim on. That's only possible once any previous claim on it has been revoked (or none ever existed). Meaning, if the previous owner still had one, it's already gone. Pick the namespace below that already publishes artifacts under this groupId; submitting sends them an accept/reject decision."
              description="Explanation of the ownership transfer preconditions shown above the request form."
            />
          }
        />

        {problem && (
          <SimpleAlert
            variant="destructive"
            title={problem.title}
            description={problem.detail}
          />
        )}

        <form.AppForm>
          <form name="request-transfer" className="grid gap-6" onSubmit={handleSubmit}>
            <form.AppField name="groupId" children={(field) => (
              <Field>
                <FieldLabel>
                  <GroupIdLabel/>
                </FieldLabel>
                <FieldDescription>
                  <FormattedMessage
                    defaultMessage="The Maven groupId this namespace has verified and wants to take over."
                    description="Hint text for the groupId combobox field on the ownership transfer request creation form."
                  />
                </FieldDescription>
                <GroupIdCombobox
                  namespace={namespace}
                  value={field.state.value}
                  onChange={(next) => {
                    field.handleChange(next);
                    form.setFieldValue('fromNamespace', '');
                  }}
                />
                <FieldError errors={field.state.meta.errors.map(normalizeFieldError)} />
              </Field>
            )}/>

            <form.AppField name="fromNamespace" children={(field) => (
              <Field>
                <FieldLabel>
                  <FormattedMessage
                    defaultMessage="From namespace"
                    description="Label for the from-namespace radio field on the ownership transfer request creation form."
                  />
                </FieldLabel>
                <FieldDescription>
                  <FormattedMessage
                    defaultMessage="The namespace that currently owns artifacts under this groupId and is being asked to release them."
                    description="Hint text for the from-namespace radio field on the ownership transfer request creation form."
                  />
                </FieldDescription>
                <form.Subscribe
                  selector={(state) => state.values.groupId}
                  children={(groupId) => (
                    <FromNamespaceRadioGroup
                      namespace={namespace}
                      groupId={groupId}
                      value={field.state.value}
                      onChange={field.handleChange}
                    />
                  )}
                />
                <FieldError errors={field.state.meta.errors.map(normalizeFieldError)} />
              </Field>
            )}/>

            <div className="flex gap-3">
              <form.Submit>
                <RequestTransferLabel/>
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
