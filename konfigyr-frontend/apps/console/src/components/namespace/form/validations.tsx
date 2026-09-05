import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { checkNamespaceQuery } from '@konfigyr/hooks';

import type { QueryClient } from '@tanstack/react-query';
import type { DeepValue, FieldValidateFn } from '@tanstack/react-form';

export async function validateSlug(queryClient: QueryClient, slug?: string, existing?: string) {
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

export function useValidateSlug<
  TParentData extends { slug: string } = { slug: string },
  TData extends DeepValue<TParentData, 'slug'> = DeepValue<TParentData, 'slug'>,
>(
  queryClient: QueryClient,
  existing?: string,
): FieldValidateFn<TParentData, 'slug', TData> {
  return useCallback(
    ({ value }) => validateSlug(queryClient, value as string, existing),
    [queryClient, existing],
  );
}
