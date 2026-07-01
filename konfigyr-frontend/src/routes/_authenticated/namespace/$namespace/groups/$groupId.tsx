import { createFileRoute } from '@tanstack/react-router';
import { useNamespace } from '@konfigyr/hooks';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { GroupsBreadcrumbs } from '@konfigyr/components/groups/breadcrumbs';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/groups/$groupId',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const { groupId } = Route.useParams();

  return (
    <LayoutContent>
      <LayoutNavbar title={groupId} />
      <div className="mx-4 space-y-6">
        <GroupsBreadcrumbs namespace={namespace}>
          {groupId}
        </GroupsBreadcrumbs>
      </div>
    </LayoutContent>
  );
}
