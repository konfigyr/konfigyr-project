import { createFileRoute } from '@tanstack/react-router';
import { proxy } from '@konfigyr/lib/api-proxy';

export const Route = createFileRoute('/api/$')({
  ssr: true,
  preload: false,
  server: {
    handlers: ({ createHandlers }) =>
      createHandlers({
        HEAD: ({ request }) => proxy(request),
        GET: ({ request }) => proxy(request),
        POST: ({ request }) => proxy(request),
        PUT: ({ request }) => proxy(request),
        PATCH: ({ request }) => proxy(request),
        DELETE: ({ request }) => proxy(request),
      }),
  },
});
