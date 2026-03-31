import { z } from 'zod';
import { toast } from 'sonner';
import { useCallback, useId, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { Link2Icon } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { useUpdateNamespace } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
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
import { useValidateSlug } from './validations';

import type { ComponentProps, SubmitEvent } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

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
  const validateSlug = useValidateSlug(queryClient, namespace.slug);

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

  const onSubmit = useCallback((event: SubmitEvent<HTMLFormElement>) => {
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
                onChangeAsync: validateSlug,
              }}
              children={(field) => (
                <field.Control
                  description={<SlugExample />}
                  render={
                    <field.Input aria-labelledby={`label-slug-${id}`} aria-describedby={`help-slug-${id}`} />
                  }
                />
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
              <form.Submit>
                <FormattedMessage
                  defaultMessage="Rename"
                  description="Namespace URL slug settings form submit button label"
                />
              </form.Submit>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
