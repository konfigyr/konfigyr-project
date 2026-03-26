import { Headset, SquareArrowOutUpRight } from 'lucide-react';
import { createFileRoute } from '@tanstack/react-router';
import { getRequestUrl } from '@tanstack/react-start/server';
import { createServerFn } from '@tanstack/react-start';
import { LayoutContent } from '@konfigyr/components/layout';
import {
  ContactSupport,
  GeneralErrorLink,
  OAuthErrorDetail,
  OAuthErrorTitle,
} from '@konfigyr/components/messages/globals';
import { buttonVariants } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from '@konfigyr/components/ui/card';
import completeAuthorizationHandler from './-handler';

const loader = createServerFn({ method: 'GET' })
  .handler(() => {
    const url = getRequestUrl();
    return completeAuthorizationHandler(url);
  });

export const Route = createFileRoute('/auth/code')({
  loader: async () => await loader(),
  component: OAuthError,
  ssr: true,
});

function OAuthError() {
  const error = Route.useLoaderData();

  return (
    <LayoutContent variant="fullscreen">
      <Card className="border w-[32rem]">
        <CardHeader
          title={<OAuthErrorTitle />}
          description={<OAuthErrorDetail />}
        />
        <CardContent className="space-y-2">
          {error.error && (
            <p className="text-sm">
              <span>Error code:</span> <code>{error.error}</code>
            </p>
          )}

          {error.error_description && (
            <p className="text-sm text-muted-foreground">
              <span>Error description:</span> {error.error_description}
            </p>
          )}

          {error.error_uri && (
            <GeneralErrorLink className={buttonVariants({ variant: 'ghost' })}>
              <SquareArrowOutUpRight size="1rem"/>
            </GeneralErrorLink>
          )}
        </CardContent>
        <CardFooter className="justify-between">
          {error.error_uri && (
            <a href={error.retry_uri!} className={buttonVariants()}>
              Retry
            </a>
          )}
          <a href="mailto:support@konfigyr.com" target="_blank" className={buttonVariants({ variant: 'outline' })}>
            <Headset size="1rem"/> <ContactSupport />
          </a>
        </CardFooter>
      </Card>
    </LayoutContent>
  );
}
