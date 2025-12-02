import { Headset, SquareArrowOutUpRight } from 'lucide-react';
import { createFileRoute } from '@tanstack/react-router';
import { getRequestUrl } from '@tanstack/react-start/server';
import { createServerFn } from '@tanstack/react-start';
import {
  ContactSupport,
  GeneralErrorLink,
  OAuthErrorDetail,
  OAuthErrorTitle,
} from '@konfigyr/components/messages/globals';
import { Button } from '@konfigyr/components/ui/button';
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
    <div className="container my-12">
      <div className="w-full lg:w-1/2 xl:w-2/5 mx-auto">
        <Card className="border">
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
              <Button variant="ghost" asChild>
                <GeneralErrorLink>
                  <SquareArrowOutUpRight size="1rem"/>
                </GeneralErrorLink>
              </Button>
            )}
          </CardContent>
          <CardFooter className="justify-between">
            {error.error_uri && (
              <Button asChild>
                <a href={error.retry_uri!}>
                  Retry
                </a>
              </Button>
            )}
            <Button variant="outline" asChild>
              <a href="mailto:support@konfigyr.com" target="_blank">
                <Headset size="1rem"/> <ContactSupport />
              </a>
            </Button>
          </CardFooter>
        </Card>
      </div>
    </div>
  );
}
