import { useNamespace } from '@konfigyr/hooks';
import { Link, createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/namespace/$namespace/')({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();

  return (
    <div>
      <h1>{namespace.name}</h1>
      <p>{namespace.description || 'No description provided'}</p>

      <div className="flex flex-col gap-2 my-2">
        <Link to="/namespace/provision">Create new namespace</Link>
        <Link to="/account">Account settings</Link>
      </div>
    </div>
  );
}
