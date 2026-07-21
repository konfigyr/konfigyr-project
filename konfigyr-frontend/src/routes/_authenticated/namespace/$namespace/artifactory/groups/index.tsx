import { z } from 'zod';
import { useCallback } from 'react';
import { PlusIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router';
import { useGetGroupVerifications, useNamespace } from '@konfigyr/hooks';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { Button, buttonVariants } from '@konfigyr/components/ui/button';
import { GroupVerificationFilters } from '@konfigyr/components/artifactory/groups/group-verification-filters';
import { GroupVerificationTable } from '@konfigyr/components/artifactory/groups/group-verification-table';

import type { GroupVerificationQuery } from '@konfigyr/hooks/types';

const searchQuerySchema = z.object({
  term: z.string().min(2).optional().catch(undefined),
  page: z.number().optional().catch(undefined),
  size: z.number().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/groups/',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const query: GroupVerificationQuery = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const { data, isPending, error } = useGetGroupVerifications(namespace.slug, query);

  const onQueryChange = useCallback(async (value: GroupVerificationQuery) => {
    await navigate({
      search: current => ({ ...current, ...value, page: 1 }),
      viewTransition: false,
    });
  }, []);

  return (
    <LayoutContent>
      <LayoutNavbar title="Group claims"/>
      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <p className="text-sm text-muted-foreground max-w-2xl">
          <FormattedMessage
            defaultMessage="Verify ownership of Maven groupId coordinates so this namespace can publish artifact metadata under them."
            description="Description of the group claims page."
          />
        </p>

        <div className="flex justify-between items-center gap-4">
          <GroupVerificationFilters
            query={query}
            onQueryChange={onQueryChange}
          />

          <Link
            to="/namespace/$namespace/artifactory/groups/create"
            params={{ namespace: namespace.slug }}
            className={buttonVariants({ variant: 'ghost' })}
          >
            <Button>
              <PlusIcon data-icon="inline-start" />
              <FormattedMessage
                defaultMessage="Claim a groupId"
                description="Label of the button that starts a new group verification claim."
              />
            </Button>
          </Link>
        </div>

        <GroupVerificationTable
          namespace={namespace.slug}
          data={data}
          isPending={isPending}
          error={error}
          page={query.page}
          size={query.size}
        />
      </div>
    </LayoutContent>
  );
}
