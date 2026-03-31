import { useInsertionEffect, useMemo } from 'react';
import { Headset } from 'lucide-react';
import { normalizeError } from '@konfigyr/components/error/normalize';
import { ContactSupport } from '@konfigyr/components/messages';
import { buttonVariants } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from '@konfigyr/components/ui/card';
import { createLogger } from '@konfigyr/logger';
import { Link, useLocation } from '@tanstack/react-router';

import type { ErrorComponentProps } from '@tanstack/react-router';

const logger = createLogger('error');

export function ErrorBoundary({ error, reset }: ErrorComponentProps) {
  const normalized = useMemo(() => normalizeError(error), [error]);
  const location = useLocation();

  useInsertionEffect(() => {
    logger.error({ error },`Unexpected error occurred while rendering page: ${location.href}`);
  });

  return (
    <div className="container my-12">
      <div className="w-full lg:w-1/2 xl:w-2/5 mx-auto">
        <Card className="border">
          <CardHeader
            title={normalized.title}
            description={normalized.detail}
          />
          {process.env.NODE_ENV !== 'production' && (
            <CardContent>
              <pre className="bg-destructive-foreground text-destructive text-xs h-[24rem] p-2 border rounded-sm leading-relaxed overflow-auto">
                <code className="block font-medium leading-loose">{error.message}</code>
                <code>{error.stack}</code>
              </pre>
            </CardContent>
          )}
          <CardFooter className="justify-between">
            <Link to="/" className={buttonVariants()} onClick={reset}>
              Home
            </Link>
            <a href="mailto:support@konfigyr.com" target="_blank" className={buttonVariants({ variant: 'outline' })}>
              <Headset size="1rem"/> <ContactSupport />
            </a>
          </CardFooter>
        </Card>
      </div>
    </div>
  );
}
