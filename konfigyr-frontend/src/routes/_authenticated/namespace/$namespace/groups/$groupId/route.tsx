import { Outlet, createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/groups/$groupId',
)({
  component: RouteComponent,
});

function RouteComponent () {
  return <Outlet />;
}
