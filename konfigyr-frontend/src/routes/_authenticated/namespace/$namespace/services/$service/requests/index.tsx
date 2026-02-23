import { EmptyState } from '@konfigyr/components/ui/empty';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/requests/',
)({
  component: RouteComponent,
});

function RouteComponent() {
  return (
    <EmptyState
      title="There are no change requests yet."
      description="You can create a change request by editing a configuration."
    />
  );
}
