import { createFileRoute } from '@tanstack/react-router';


export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/groups/create',
)({
  component: RouteComponent,
});

function RouteComponent() {
  return (
    <h1>Create</h1>
  );
}
