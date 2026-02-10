import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/$profile',
)({
  component: RouteComponent,
});

function RouteComponent() {
  return (
    <div>
      Hello "/_authenticated/namespace/$namespace/services/$service/$profile"!
    </div>
  );
}
