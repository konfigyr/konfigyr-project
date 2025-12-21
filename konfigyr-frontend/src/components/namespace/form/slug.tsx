import { z } from 'zod';
import { toast } from 'sonner';
import { useCallback, useId, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { Link2Icon } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { checkNamespaceQuery, useUpdateNamespace } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardAction,
  CardContent,
  CardFooter,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { useFieldContext, useForm } from '@konfigyr/components/ui/form';
import { cn } from '@konfigyr/components/utils';
import { NamespaceSlugDescription } from './messages';

import type { ComponentProps, FormEvent } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';
import type { QueryClient } from '@tanstack/react-query';

export async function validateSlug(queryClient: QueryClient, slug: string, existing?: string) {
  if (!slug || slug === existing) {
    return null;
  }

  let exists;

  try {
    const result = await queryClient.ensureQueryData(checkNamespaceQuery(slug));
    exists = result.exists;
  } catch (error) {
    exists = false;
  }

  if (exists) {
    return (
      <FormattedMessage
        defaultMessage="Namespace with the following URL already exists, please choose another one."
        description="Error message that is shown when user tries to create a namespace with a slug that already exists."
      />
    );
  }

  return null;
}

export function SlugExample({ className, ...props }: ComponentProps<'span'>) {
  const field = useFieldContext<string>();

  const url = useMemo(
    () => new URL(`/namespace/${field.state.value || 'namespace-name'}`, window.location.origin),
    [field.state.value, window.location.origin],
  );

  return (
    <span className={cn('block', className)} {...props}>
      <FormattedMessage
        defaultMessage="Your URL will be: <example>{url}</example>"
        description="Namespace URL slug preview text via: `<example>{url}</example>`"
        values={{
          example: (chunks) => (
            <span className="font-medium" key="example">
              {chunks}
            </span>
          ),
          url: url.href,
        }}
      />
    </span>
  );
}

export function SlugDescription() {
  return (
    <>
      <NamespaceSlugDescription />
      <SlugExample className="mt-1"/>
    </>
  );
}

const namespaceSlugSchema = z.object({
  slug: z.string()
    .min(5, { message: 'URL slug must be at least 5 characters.' })
    .max(30, { message: 'URL slug must be at most 30 characters.' }),
});

export function NamespaceSlugForm({ namespace }: { namespace: Namespace }) {
  const id = useId();
  const queryClient = useQueryClient();
  const { mutateAsync: updateNamespace } = useUpdateNamespace(namespace);
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: {
      slug: namespace.slug || '',
    },
    validators: {
      onChange: namespaceSlugSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await updateNamespace(value);
      } catch (error) {
        return errorNotification(error);
      }

      return toast.success((
        <FormattedMessage
          defaultMessage="Your namespace was successfully renamed"
          description="Notification message that is shown when namespace URL slug was successfully updated"
        />
      ));
    },
  });

  const onSubmit = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();

    return form.handleSubmit(event);
  }, [form.handleSubmit]);

  return (
    <form.AppForm>
      <form name="namespace-slug-form" onSubmit={onSubmit}>
        <Card className="border">
          <CardHeader>
            <CardTitle id={`label-slug-${id}`} className="flex items-center gap-2">
              <CardIcon>
                <Link2Icon size="1.25rem"/>
              </CardIcon>
              <FormattedMessage
                defaultMessage="Rename namespace"
                description="Title for the Namespace URL slug settings form"
              />
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p id={`help-slug-${id}`} className="mb-4">
              <NamespaceSlugDescription />
            </p>

            <form.AppField
              name="slug"
              validators={{
                onChangeAsyncDebounceMs: 300,
                onChangeAsync: ({ value }) => validateSlug(queryClient, value, namespace.slug),
              }}
              children={(field) => (
                <field.Control description={<SlugExample />}>
                  <field.Input aria-labelledby={`label-slug-${id}`} aria-describedby={`help-slug-${id}`} />
                </field.Control>
              )}
            />

          </CardContent>
          <CardFooter className="justify-between border-t">
            <p className="text-muted-foreground text-sm">
              <FormattedMessage
                defaultMessage="Please use 30 characters at maximum."
                description="Namespace URL slug form field validation constraints"
              />
            </p>
            <CardAction>
              <form.Subscribe
                selector={state => [state.isValid, state.isValidating, state.isSubmitting]}
                children={([isValid, isValidating, isSubmitting]) => (
                  <Button
                    type="submit"
                    disabled={!isValid}
                    loading={isSubmitting || isValidating}
                  >
                    <FormattedMessage
                      defaultMessage="Rename"
                      description="Namespace URL slug settings form submit button label"
                    />
                  </Button>
                )}
              />
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
