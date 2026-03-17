import { createFileRoute } from '@tanstack/react-router';
import { renderErrorResponse } from '@konfigyr/lib/api-proxy';
import Authentication from '@konfigyr/lib/authentication';

export const Route = createFileRoute('/auth/scopes')({
  ssr: true,
  preload: false,
  server: {
    handlers: ({ createHandlers }) => createHandlers({
      GET: async () => {
        const authentication = await Authentication.get();

        try {
          return await fetch(`${authentication.issuer}/oauth/scope-metadata`);
        } catch (ex) {
          return renderErrorResponse({
            title: 'Internal Server Error',
            detail: 'An unexpected error occurred while proxying the request.',
            status: 500,
          });
        }
      },
    }),
  },
});
