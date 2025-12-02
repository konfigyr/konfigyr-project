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
      <p>{namespace.description}</p>
      <Link to="/namespace/provision">Create new namespace</Link>
    </div>
  );
}
