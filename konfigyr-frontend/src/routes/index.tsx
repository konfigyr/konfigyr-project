import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/')({
  preload: false,
  component: Home,
});

function Home() {
  return (
    <div className="p-2">
      <h3>Welcome to Konfigyr</h3>
    </div>
  );
}
