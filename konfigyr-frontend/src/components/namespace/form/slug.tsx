import { useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { checkNamespaceQuery } from '@konfigyr/hooks';
import { useFieldContext } from '@konfigyr/components/ui/form';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';
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
        id="namespaces.form.slug.exists"
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
        id="namespaces.form.slug.example"
        defaultMessage="Your URL will be: <example>{url}</example>"
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
      <FormattedMessage
        id="namespaces.form.slug.description"
        defaultMessage="The unique URL-friendly identifier for this namespace. Auto-generated from the name, but you can tweak it if needed. No spaces, just dashes!"
      />
      <SlugExample className="mt-1"/>
    </>
  );
}
