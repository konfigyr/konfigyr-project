import {
  getKeysetKeysQuery,
  getKeysetQuery,
  useNamespace,
} from '@konfigyr/hooks';
import { Link, createFileRoute } from '@tanstack/react-router';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@konfigyr/components/ui/breadcrumb';
import { KeysetDetails } from '@konfigyr/components/kms/keyset-details';
import { KeyManagementServiceLabel } from '@konfigyr/components/kms/messages';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/kms/$keyset',
)({
  component: RouteComponent,
  loader: async ({ context, params }) => {
    const keyset = await context.queryClient.ensureQueryData(getKeysetQuery(params.namespace, params.keyset));
    const keys = await context.queryClient.ensureQueryData(getKeysetKeysQuery(params.namespace, params.keyset));

    return { keyset, keys };
  },
});

function RouteComponent() {
  const namespace = useNamespace();
  const navigate = Route.useNavigate();
  const { keyset, keys } = Route.useLoaderData();

  return (
    <div id="keyset-details" className="mx-4 space-y-2">
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink
              render={
                <Link
                  to="/namespace/$namespace/kms"
                  params={{ namespace: namespace.slug }}
                >
                  <KeyManagementServiceLabel />
                </Link>
              }
            />
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>{keyset.name}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <KeysetDetails
        namespace={namespace}
        keyset={keyset}
        keys={keys}
        onChange={() => navigate({})}
        onDelete={() => navigate({
          to: '/namespace/$namespace/kms',
          params: { namespace: namespace.slug },
        })}
      />
    </div>
  );
}
