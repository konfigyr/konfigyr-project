import { HeadsetIcon, HomeIcon } from 'lucide-react';
import { Link } from '@tanstack/react-router';
import {
  ContactSupport,
  KonfigyrLeadMessage,
  KonfigyrTitleMessage,
} from '@konfigyr/components/messages';
import { LayoutContent } from '@konfigyr/components/layout';
import { buttonVariants } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from '@konfigyr/components/ui/card';

import type { ComponentProps, EventHandler, MouseEvent } from 'react';

export function ErrorPage({ children, ...props }: ComponentProps<typeof LayoutContent>) {
  return (
    <LayoutContent variant="fullscreen" {...props}>
      <div className="w-full lg:w-1/2 xl:w-2/5 mx-auto">
        <div className="text-center mb-6">
          <p className="text-2xl font-heading">
            <KonfigyrTitleMessage />
          </p>
          <p className="text-muted-foreground">
            <KonfigyrLeadMessage />
          </p>
        </div>

        <Card className="border">
          {children}
        </Card>
      </div>
    </LayoutContent>
  );
}

export function ErrorPageHeader(props: ComponentProps<typeof CardHeader>) {
  return (
    <CardHeader {...props} />
  );
}

export function ErrorPageContent(props: ComponentProps<typeof CardContent>) {
  return (
    <CardContent {...props} />
  );
}

export function ErrorPageStackTrace({ error }: { error: Error }) {
  return (
    <pre className="bg-destructive-foreground text-destructive text-xs h-96 p-2 border rounded-sm leading-relaxed overflow-auto">
      <code className="block font-medium leading-loose">{error.message}</code>
      <code>{error.stack}</code>
    </pre>
  );
}

export function ErrorPageFooter({ onHome, onContactSupport, ...props }: {
  onHome?: EventHandler<MouseEvent>,
  onContactSupport?: EventHandler<MouseEvent>,
} & Omit<ComponentProps<typeof CardFooter>, 'children'>) {
  return (
    <CardFooter className="justify-between" {...props}>
      <Link to="/" className={buttonVariants({ variant: 'default' })} onClick={onHome}>
        <HomeIcon /> Home
      </Link>
      <a
        href="mailto:support@konfigyr.com"
        target="_blank"
        className={buttonVariants({ variant: 'outline' })}
        onClick={onContactSupport}
      >
        <HeadsetIcon /> <ContactSupport />
      </a>
    </CardFooter>
  );
}
