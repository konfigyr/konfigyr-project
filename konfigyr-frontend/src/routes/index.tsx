import { createFileRoute } from '@tanstack/react-router';
import { useAccount } from '@konfigyr/hooks';

export const Route = createFileRoute('/')({
  preload: false,
  component: Home,
});

function Home() {
  const account = useAccount();

  return (
    <div className="p-2">
      <h3>Welcome to Konfigyr</h3>
      <p>Hello: {account.fullName}</p>
    </div>
  );
}
