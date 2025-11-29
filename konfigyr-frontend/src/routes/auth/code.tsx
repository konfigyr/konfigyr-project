import { createFileRoute, redirect } from '@tanstack/react-router';
import { getRequestUrl } from '@tanstack/react-start/server';
import { createServerFn } from '@tanstack/react-start';
import Authentication from '@konfigyr/lib/authentication';
import { createLogger } from '@konfigyr/logger';

const logger = createLogger('oauth/code');

const loader = createServerFn({ method: 'GET' })
  .handler(async () => {
    const authentication = await Authentication.get();

    let redirectUri: URL;

    try {
      redirectUri = await authentication.completeAuthorization(getRequestUrl());
    } catch (ex) {
      logger.error({ stack: ex }, 'Failed to exchange authorization code');

      return { error: 'Failed to exchange authorization code' };
    }

    throw redirect({ href: redirectUri.href });
  });

export const Route = createFileRoute('/auth/code')({
  loader: async () => await loader(),
  component: OAuthError,
  ssr: true,
});

function OAuthError() {
  const state = Route.useLoaderData();

  return (
    <p>{state.error}</p>
  );
}
